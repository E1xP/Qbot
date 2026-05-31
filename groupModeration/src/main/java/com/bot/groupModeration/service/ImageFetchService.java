package com.bot.groupModeration.service;

import com.bot.groupModeration.util.CqImageParser;
import com.bot.retdata.ApiData;
import com.bot.retdata.FileData;
import com.bot.robot.CoolQ;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.Optional;

/**
 * 从 CQ 图片段获取原始字节。
 * <p>
 * 拉取顺序：segment.url（http/file/本地路径）→ segment.file 本地路径 → OneBot API
 *（{@code [CQ:file]} 上传优先 {@code get_file}，否则 {@code get_image}）。
 */
@Service
@Slf4j
public class ImageFetchService {

    @Resource
    private RestTemplate restTemplate;

    /**
     * 根据 CQ file/url 或文件头推断扩展名，用于落盘文件名
     */
    public static String guessSuffix(byte[] bytes, String fileHint, String urlHint) {
        String fromPath = CqImageParser.extractImageSuffix(fileHint);
        if (fromPath == null) {
            fromPath = CqImageParser.extractImageSuffix(urlHint);
        }
        if (fromPath != null) {
            return fromPath;
        }
        return guessSuffixByMagic(bytes);
    }

    /**
     * 仅从文件头推断（无路径提示时）
     */
    public static String guessSuffix(byte[] bytes) {
        return guessSuffix(bytes, null, null);
    }

    private static String guessSuffixByMagic(byte[] bytes) {
        if (bytes == null || bytes.length < 3) {
            return ".jpg";
        }
        // JPEG / JFIF
        if (bytes.length > 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return ".jpg";
        }
        // PNG
        if (bytes.length > 3 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return ".png";
        }
        // GIF
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return ".gif";
        }
        // WebP: RIFF....WEBP
        if (bytes.length > 11 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return ".webp";
        }
        // BMP
        if (bytes[0] == 'B' && bytes[1] == 'M') {
            return ".bmp";
        }
        return ".jpg";
    }

    /**
     * 拉取单张图片内容。
     *
     * @param cq      机器人实例
     * @param segment 解析出的 CQ image 段
     * @return 图片字节，失败为空
     */
    public Optional<byte[]> fetch(CoolQ cq, CqImageParser.CqImageSegment segment) {
        Optional<byte[]> fromUrl = fetchFromUrl(segment.getUrl());
        if (fromUrl.isPresent()) {
            return fromUrl;
        }
        if (segment.getFile() != null && !segment.getFile().isEmpty()) {
            return fetchFromFileParam(cq, segment.getFile(), segment.isFileUpload());
        }
        return Optional.empty();
    }

    private Optional<byte[]> fetchFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return Optional.empty();
        }
        try {
            if (url.startsWith("file:")) {
                String path = url.replace("file:///", "").replace("file://", "");
                if (path.startsWith("/") && path.contains(":")) {
                    path = path.substring(1);
                }
                File file = new File(path);
                if (file.isFile()) {
                    return Optional.of(Files.readAllBytes(file.toPath()));
                }
                return Optional.empty();
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                byte[] bytes = restTemplate.getForObject(URI.create(url), byte[].class);
                return bytes == null || bytes.length == 0 ? Optional.empty() : Optional.of(bytes);
            }
            File local = new File(url);
            if (local.isFile()) {
                return Optional.of(Files.readAllBytes(local.toPath()));
            }
        } catch (Exception e) {
            log.warn("拉取图片失败 url={}", url, e);
        }
        return Optional.empty();
    }

    private Optional<byte[]> fetchFromFileParam(CoolQ cq, String fileParam, boolean fileUpload) {
        if (fileParam.startsWith("http://") || fileParam.startsWith("https://")) {
            return fetchFromUrl(fileParam);
        }
        if (fileParam.startsWith("file:")) {
            return fetchFromUrl(fileParam);
        }
        File local = new File(fileParam);
        if (local.isFile()) {
            try {
                return Optional.of(Files.readAllBytes(local.toPath()));
            } catch (Exception e) {
                log.warn("读取本地图片失败 file={}", fileParam, e);
            }
        }
        return fetchFromProtocolApi(cq, fileParam, fileUpload);
    }

    private Optional<byte[]> fetchFromProtocolApi(CoolQ cq, String fileParam, boolean fileUpload) {
        try {
            if (fileUpload) {
                Optional<byte[]> fromFile = resolveFileData(cq.getFile(fileParam));
                if (fromFile.isPresent()) {
                    return fromFile;
                }
                return resolveFileData(cq.getImage(fileParam));
            }
            Optional<byte[]> fromImage = resolveFileData(cq.getImage(fileParam));
            if (fromImage.isPresent()) {
                return fromImage;
            }
            return resolveFileData(cq.getFile(fileParam));
        } catch (Exception e) {
            log.warn("协议端拉取图片失败 file={} fileUpload={}", fileParam, fileUpload, e);
            return Optional.empty();
        }
    }

    private Optional<byte[]> resolveFileData(ApiData<FileData> apiData) {
        if (apiData == null || apiData.getData() == null) {
            return Optional.empty();
        }
        FileData data = apiData.getData();
        if (data.getUrl() != null) {
            Optional<byte[]> bytes = fetchFromUrl(data.getUrl());
            if (bytes.isPresent()) {
                return bytes;
            }
        }
        if (data.getFile() != null) {
            return fetchFromUrl(data.getFile());
        }
        return Optional.empty();
    }
}
