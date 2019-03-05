/*
 * matrix-appservice-email - Matrix Bridge to E-mail
 * Copyright (C) 2017 Kamax Sarl
 *
 * https://www.kamax.io/
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
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
        recvPattern = Pattern.compile(recv.getEmail().replace("+", "\\+").replace("%KEY%", "(?<" + keyGroupName + ">.+?)"));
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
            log.info("Closing folder {}", folder.getName());
            folder.close(true);
        } catch (Exception e) {
            log.warn("Error when closing folder", e);
        } finally {
            try {
                folder = null;

                log.info("Closing store {}", store.getURLName());
                store.close();
            } catch (Exception e) {
                log.warn("Error when closing store", e);
            } finally {
                store = null;
            }
        }
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

                                if (message.getFrom().length > 0) {
                                    Address[] recipients = message.getAllRecipients();
                                    for (Address recipient : recipients) {
                                        InternetAddress address = (InternetAddress) recipient;
                                        Matcher m = recvPattern.matcher(address.getAddress());
                                        if (m.matches()) {
                                            String key = m.group(keyGroupName);
                                            log.info("Got email with key {}", key);

                                            for (_EmailMessageListener listener : listeners) {
                                                listener.push(key, message);
                                            }

                                            break;
                                        }
                                    }
                                } else {
                                    log.info("Received unsupported email: no sender");
                                }

                                message.setFlag(Flags.Flag.DELETED, true);
                            }

                            folder.expunge();
                        }
                    } catch (InterruptedException e) {
                        log.info("Email receiver thread was interrupted");
                    } catch (MessagingException e) {
                        log.error("Error in e-mail backend: {}", e.getMessage());
                        doDisconnect();
                    } catch (Throwable t) {
                        log.error("Error in e-mail fetcher", t);
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
