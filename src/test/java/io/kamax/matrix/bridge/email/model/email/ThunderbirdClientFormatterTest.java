package io.kamax.matrix.bridge.email.model.email;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class ThunderbirdClientFormatterTest {

    private ThunderbirdClientFormatter formatter = new ThunderbirdClientFormatter();

    @Test
    public void testPlain() throws IOException {
        File f = new File("src/test/resources/thunderbirdContent.txt");
        assertTrue(f.getAbsolutePath(), f.canRead());

        String output = formatter.formatPlain(IOUtils.toString(new FileReader(f)));
        assertTrue(output, "orly".contentEquals(output));
    }

    @Test
    public void testHtml() throws IOException {
        File f = new File("src/test/resources/thunderbirdContent.html");
        assertTrue(f.getAbsolutePath(), f.canRead());

        String output = formatter.formatHtml(IOUtils.toString(new FileReader(f)));
        assertTrue(output, "orly".contentEquals(output));
    }

}
