package com.xurx.springai;


import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;

import java.util.List;

public class TestPDF {
    @Test
    public void getPDF() {
        // 解析论文
        var pdf = new FileSystemResource("/Users/xurx/Downloads/dfg_for_3D_object_detection0116.pdf");
        var reader = new TikaDocumentReader(pdf);

        List<Document> docs = reader.get(); // 解析 PDF => Document 列表
        System.out.println("docs = " + docs.size());
        for (Document d : docs) {
            if (d.isText()) {
                System.out.println(d.getText());
            } else {
                System.out.println(d.getMedia());
            }
        }
    }
}
