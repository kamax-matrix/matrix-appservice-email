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

package io.kamax.matrix.bridge.email.model;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BridgeEmailCodec {


    public static final String delimiter = "=";
    public static final String charsToReplaceRegex = "[^0-9a-z-._]+";
    public static final Pattern charsToReplacePattern = Pattern.compile(charsToReplaceRegex);
    public static final Pattern decodePattern = Pattern.compile("(=[0-9a-f]{2})+");

    public String decode(String valueEncoded) {
        StringBuilder builder = new StringBuilder();

        Matcher m = decodePattern.matcher(valueEncoded);
        int prevEnd = 0;
        while (m.find()) {
            try {
                int start = m.start();
                int end = m.end();
                String sub = valueEncoded.substring(start, end).replaceAll(delimiter, "");
                String decoded = new String(Hex.decodeHex(sub.toCharArray()), StandardCharsets.UTF_8);
                builder.append(valueEncoded.substring(prevEnd, start));
                builder.append(decoded);
                prevEnd = end - 1;
            } catch (DecoderException e) {
                e.printStackTrace();
            }
        }
        prevEnd++;
        if (prevEnd < valueEncoded.length()) {
            builder.append(valueEncoded.substring(prevEnd, valueEncoded.length()));
        }

        if (builder.length() == 0) {
            return valueEncoded;
        } else {
            return builder.toString();
        }
    }

    public String encode(String value) {
        value = value.toLowerCase();

        StringBuilder builder = new StringBuilder();
        for (Character c : value.toCharArray()) {
            String s = c.toString();
            Matcher lp = charsToReplacePattern.matcher(s);
            if (!lp.find()) {
                builder.append(s);
            } else {
                for (byte b : c.toString().getBytes(StandardCharsets.UTF_8)) {
                    builder.append(delimiter);
                    builder.append(Hex.encodeHexString(new byte[]{b}));
                }
            }
        }

        return builder.toString();
    }

}
