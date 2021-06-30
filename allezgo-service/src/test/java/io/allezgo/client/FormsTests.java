package io.allezgo.client;

import org.junit.jupiter.api.Test;

final class FormsTests {
    @Test
    public void testMultipartFormatting() {
        Forms.MultipartUpload up = Forms.MultipartUpload.of("filename.tcx", "content");
        System.out.println(up);
    }
}
