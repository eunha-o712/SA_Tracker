package com.sa.trk.board.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class BoardNoticeHtmlSanitizer {

    private static final Safelist NOTICE_SAFELIST = Safelist.none()
            .addTags(
                    "section", "header", "article", "div",
                    "h2", "h3", "p", "ol", "ul", "li",
                    "strong", "span", "small", "br")
            .addAttributes(":all", "class", "aria-label", "aria-hidden");

    private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings()
            .prettyPrint(false);

    public String sanitize(String html) {
        if (html == null) return null;
        return Jsoup.clean(html, "", NOTICE_SAFELIST, OUTPUT_SETTINGS);
    }
}
