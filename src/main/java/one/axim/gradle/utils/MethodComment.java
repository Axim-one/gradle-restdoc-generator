package one.axim.gradle.utils;

import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MethodComment {
    private Javadoc javadoc;
    private List<CommentTag> tags = new ArrayList<>();

    private MethodComment(Javadoc javadoc) {
        this.javadoc = javadoc;
        parse();
    }

    public static MethodComment wrap(Javadoc javadoc) {
        return new MethodComment(javadoc);
    }

    private void parse() {
        for (JavadocBlockTag blockTag : javadoc.getBlockTags()) {
            String tagName = blockTag.getTagName();
            String name = null;
            String value;

            // javaparser가 이름을 자동 추출하는 태그 (param, throws 등)
            Optional<String> tagParamName = blockTag.getName();
            if (tagParamName.isPresent()) {
                name = tagParamName.get();
                value = blockTag.getContent().toText().trim();
            } else {
                // 커스텀 태그 (response, header 등): 첫 번째 단어를 name으로 분리
                String content = blockTag.getContent().toText().trim();
                if ("response".equals(tagName) || "header".equals(tagName) || "error".equals(tagName)) {
                    int pos = content.indexOf(" ");
                    if (pos != -1) {
                        name = content.substring(0, pos);
                        value = content.substring(pos + 1).trim();
                    } else {
                        name = content;
                        value = "";
                    }
                } else {
                    value = content;
                }
            }

            tags.add(new CommentTag(tagName, name, value));
        }
    }

    public String getDescription() {
        String desc = javadoc.getDescription().toText().trim();
        desc = desc.replaceAll("\\n", "<br>");
        return desc;
    }

    public List<CommentTag> getTags() {
        return tags;
    }
}
