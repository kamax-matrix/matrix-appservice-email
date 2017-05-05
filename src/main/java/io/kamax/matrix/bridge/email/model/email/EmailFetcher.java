/*
 * matrix-appservice-email - Matrix Bridge to E-mail
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.matrix.bridge.email.model.email;

import io.kamax.matrix.bridge.email.config.email.EmailReceiverConfig;
import io.kamax.matrix.bridge.email.model.BridgeMessageHtmlContent;
import io.kamax.matrix.bridge.email.model.BridgeMessageTextContent;
import io.kamax.matrix.bridge.email.model._BridgeMessageContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EmailFetcher implements _EmailFetcher, InitializingBean {

    private Logger log = LoggerFactory.getLogger(EmailFetcher.class);

    @Autowired
    private EmailReceiverConfig recv;

    private final int sleepTime = 1000;
    private final String keyGroupName = "key";
    private Store store;
    private Folder folder;
    private Thread runner;
    private Pattern recvPattern;

    private List<_EmailMessageListener> listeners;

    @Override
    public void afterPropertiesSet() throws Exception {
        recvPattern = Pattern.compile(recv.getTemplate().replace("+", "\\+").replace("%KEY%", "(?<" + keyGroupName + ">.+?)"));
        listeners = new ArrayList<>();
    }

    private void doConnect() {
        try {
            if (store != null && store.isConnected() && folder != null && folder.isOpen()) {
                return;
            }

            if (store == null || !store.isConnected()) {
                Session session = Session.getDefaultInstance(System.getProperties());
                store = session.getStore(recv.getType());
                store.connect(recv.getHost(), recv.getPort(), recv.getLogin(), recv.getPassword());
            }

            if (folder != null && folder.isOpen()) {
                folder.close(true);
                folder = null;
            }

            if (folder == null) {
                folder = store.getFolder("inbox");
                folder.open(Folder.READ_WRITE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doDisconnect() {
        try {
            folder.close(true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                folder = null;
                store.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                store = null;
            }
        }
    }

    protected List<_BridgeMessageContent> extractContent(Part p) throws MessagingException, IOException {
        if (p.isMimeType("multipart/*")) {
            log.info("Found multipart content, extracting");

            List<_BridgeMessageContent> contents = new ArrayList<>();
            Multipart mp = (Multipart) p.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                contents.addAll(extractContent(mp.getBodyPart(i)));
            }
            return contents;
        }

        if (p.isMimeType("message/rfc822")) {
            log.info("Found nested content, extracting");
            return extractContent((Part) p.getContent());
        }

        List<_BridgeMessageContent> contents = new ArrayList<>();
        if (p.isMimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)) {
            log.info("Found plain text content");
            return Collections.singletonList(new BridgeMessageTextContent((String) p.getContent()));
        }

        if (p.isMimeType(MimeTypeUtils.TEXT_HTML_VALUE)) {
            log.info("Found HTML content");
            return Collections.singletonList(new BridgeMessageHtmlContent((String) p.getContent()));
        }

        return Collections.emptyList();
    }

    @Override
    public void connect() {
        log.info("Connect: start");
        try {
            doConnect();

            runner = new Thread(() -> {
                log.info("Email receiver thread: start");

                while (!Thread.interrupted()) {
                    try {
                        doConnect();
                        while (store.isConnected()) {
                            Thread.sleep(sleepTime);

                            Message[] messages = folder.getMessages();
                            for (Message message : messages) {
                                if (message.isExpunged() || message.getFlags().contains(Flags.Flag.DELETED)) {
                                    continue;
                                }

                                Address[] recipients = message.getAllRecipients();
                                for (Address recipient : recipients) {
                                    InternetAddress address = (InternetAddress) recipient;
                                    Matcher m = recvPattern.matcher(address.getAddress());
                                    if (!m.matches()) {
                                        log.info("Received unsupported email");
                                    } else {
                                        String key = m.group(keyGroupName);
                                        String sender = ((InternetAddress) message.getFrom()[0]).getAddress(); // TODO sanitize properly
                                        log.info("Got email with key {} from {}", key, sender);

                                        log.info("Getting email content");
                                        List<_BridgeMessageContent> contents = extractContent(message);
                                        if (contents.isEmpty()) {
                                            log.warn("Found no valid content, skipping");
                                        } else {
                                            EmailBridgeMessage email = new EmailBridgeMessage(key, sender, contents);
                                            for (_EmailMessageListener listener : listeners) {
                                                try {
                                                    listener.push(email);
                                                } catch (Throwable t) {
                                                    log.error("Error when dispatching e-mail to listener", t);
                                                }
                                            }
                                        }
                                    }

                                    message.setFlag(Flags.Flag.DELETED, true);
                                }
                            }

                            folder.expunge();
                        }
                    } catch (InterruptedException e) {
                        log.info("Email receiver thread was interrupted");
                    } catch (MessagingException | IOException e) {
                        doDisconnect();
                    }
                }

                log.info("Email receiver thread: stop");
            });
            runner.setName("email-receiver-daemon");
            runner.setDaemon(true);
            runner.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            log.info("Connect: end");
        }
    }

    @Override
    public void disconnect() {
        log.info("Disconnect: start");

        log.info("Disconnect: interrupt receiver daemon");
        runner.interrupt();
        try {
            log.info("Disconnect: receiver daemon join: start");
            runner.join(sleepTime * 5L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            log.info("Disconnect: receiver daemon join: end");
        }

        doDisconnect();

        log.info("Disconnect: end");
    }

    @Override
    public void addListener(_EmailMessageListener listener) {
        listeners.add(listener);
    }

}
