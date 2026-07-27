package com.god.mz.domain.vo.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadVO {

    private String url;

    private String fileName;

    private Long fileSize;
}
