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

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class GmailClientFormatterTest {

    private GmailClientFormatter formatter = new GmailClientFormatter();

    @Test
    public void testPlain() throws IOException {
        File f = new File("src/test/resources/gmailContent.txt");
        assertTrue(f.getAbsolutePath(), f.canRead());

        String output = formatter.formatPlain(IOUtils.toString(new FileReader(f)));
        assertTrue(output, "a".contentEquals(output));
    }

    @Test
    public void testHtml() throws IOException {
        File f = new File("src/test/resources/gmailContent.html");
        assertTrue(f.getAbsolutePath(), f.canRead());

        String output = formatter.formatHtml(IOUtils.toString(new FileReader(f)));
        assertTrue(output, "a".contentEquals(output));
    }

}
