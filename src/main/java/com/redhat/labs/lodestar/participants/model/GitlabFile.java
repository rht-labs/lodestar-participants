package com.redhat.labs.lodestar.participants.model;

import java.nio.charset.StandardCharsets;

import javax.validation.constraints.NotBlank;

import com.redhat.labs.lodestar.participants.utils.EncodingUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitlabFile {

    @NotBlank private String filePath;
    @NotBlank private String fileName;
    @NotBlank private String ref;
    @NotBlank private String content;
    @NotBlank private String lastCommitId;
    @Builder.Default 
    private String encoding = "base64";
    private Long size;
    private String branch;
    private String authorEmail;
    private String authorName;
    private String commitMessage;

    public void encodeFileAttributes() {
        this.filePath = EncodingUtils.urlEncode(this.filePath);

        // encode contents
        if (null != content) {
            byte[] encodedContents = EncodingUtils.base64Encode(this.content.getBytes());
            this.content = new String(encodedContents, StandardCharsets.UTF_8);
        }
    }

    public void decodeFileAttributes() {
        System.out.println(this);
        this.filePath = EncodingUtils.urlDecode(this.filePath);

        // decode contents
        if (null != content) {
            byte[] decodedContents = EncodingUtils.base64Decode(this.content);
            this.content = new String(decodedContents, StandardCharsets.UTF_8);

        }
    }
}
