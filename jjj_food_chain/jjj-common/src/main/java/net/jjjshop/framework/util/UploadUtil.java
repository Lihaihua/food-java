

package net.jjjshop.framework.util;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.jjjshop.common.entity.file.UploadFile;
import net.jjjshop.common.enums.SettingEnum;
import net.jjjshop.common.enums.StorageEnum;
import net.jjjshop.common.factory.upload.UploadFactory;
import net.jjjshop.common.factory.upload.UploadFactoryService;
import net.jjjshop.common.settings.vo.StorageVo;
import net.jjjshop.common.util.SettingUtils;
import net.jjjshop.config.properties.SpringBootJjjProperties;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 文件上传工具类
 */
@Slf4j
@Component
public class UploadUtil {
    @Autowired
    private SettingUtils settingUtils;
    @Autowired
    private UploadFactory uploadFactory;
    @Autowired
    private SpringBootJjjProperties springBootJjjProperties;
    /**
     * 上传文件
     * @param multipartFile
     */
    public synchronized void upload(MultipartFile multipartFile, UploadFile file) throws Exception {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        StorageVo storageVo;
        // 获取当前的HttpServletRequest对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        // 请求路径 /api/admin/file/upload/image
        String path = request.getRequestURI();
        if(path.startsWith("/api/admin/file/upload/image")){
            //获取admin端配置
            JSONObject vo = settingUtils.getShopSetting(SettingEnum.SYS_CONFIG.getKey(), 0L);
            JSONObject storage = vo.getJSONObject("storageVo");
            storageVo = JSONObject.toJavaObject(storage, StorageVo.class);
        }else {
            //获取shop端配置
            JSONObject vo = settingUtils.getShopSetting(SettingEnum.STORAGE.getKey(), null);
            storageVo = JSONObject.toJavaObject(vo, StorageVo.class);
        }

        UploadFactoryService service = uploadFactory.getService(storageVo.getCurrent());
        // 生成安全文件后缀与文件名（图片按文件头识别，不信任客户端后缀）
        String safeExtension = resolveSafeExtension(multipartFile, file);
        String saveFileName = new DefaultUploadFileNameHandleImpl().handle(safeExtension);
        // 开始上传
        service.upload(multipartFile, saveFileName);
        file.setFileName(saveFileName);
        file.setStorage(storageVo.getCurrent());
        if(StorageEnum.QINIU.getValue().equals(storageVo.getCurrent())){
            file.setFileUrl(storageVo.getQiNiu().getDomain());
        }else if(StorageEnum.ALIYUN.getValue().equals(storageVo.getCurrent())){
            file.setFileUrl(storageVo.getAliYun().getDomain());
        }else if(StorageEnum.QCLOUD.getValue().equals(storageVo.getCurrent())){
            file.setFileUrl(storageVo.getQCloud().getDomain());
        }
    }

    /**
     * 根据业务类型决定安全后缀：
     * 1) 图片：基于Magic Number识别类型，避免伪造后缀绕过
     * 2) 文档：基于文件头识别并校验与扩展名一致性
     * 3) 其他：使用原始后缀（小写）并做白名单校验
     */
    private String resolveSafeExtension(MultipartFile multipartFile, UploadFile file) throws Exception {
        List<String> allow = springBootJjjProperties.getAllowUploadFileExtensions();
        String fileType = file == null ? null : file.getFileType();
        String originalFilename = multipartFile.getOriginalFilename();
        String originalExt = StringUtils.lowerCase(FilenameUtils.getExtension(originalFilename));

        // 图片类型：完全基于文件头识别
        if ("image".equalsIgnoreCase(StringUtils.trimToEmpty(fileType))) {
            String ext = detectImageExtension(multipartFile);
            checkAllowedExtension(ext, allow);
            return ext;
        }

        // 文档类型：文件头识别 + 扩展名一致性校验
        if (isDocumentType(originalExt)) {
            String ext = detectDocumentExtension(multipartFile, originalExt);
            checkAllowedExtension(ext, allow);
            return ext;
        }

        // 其他类型：白名单校验
        if (StringUtils.isBlank(originalExt)) {
            throw new IllegalArgumentException("文件后缀不能为空");
        }
        checkAllowedExtension(originalExt, allow);
        return originalExt;
    }

    /**
     * 判断是否为文档类型
     */
    private boolean isDocumentType(String ext) {
        if (StringUtils.isBlank(ext)) {
            return false;
        }
        return "pdf".equalsIgnoreCase(ext) || "docx".equalsIgnoreCase(ext)
            || "xlsx".equalsIgnoreCase(ext) || "pptx".equalsIgnoreCase(ext);
    }

    private void checkAllowedExtension(String ext, List<String> allow) {
        if (StringUtils.isBlank(ext)) {
            throw new IllegalArgumentException("文件后缀不能为空");
        }
        if (allow == null || allow.isEmpty()) {
            return;
        }
        boolean ok = allow.stream().filter(StringUtils::isNotBlank).anyMatch(a -> ext.equalsIgnoreCase(a.trim()));
        if (!ok) {
            throw new IllegalArgumentException("不允许上传该类型文件");
        }
    }

    /**
     * 图片文件头识别：
     * - jpg: FF D8 FF
     * - png: 89 50 4E 47 0D 0A 1A 0A
     * - gif: 47 49 46 38
     */
    private String detectImageExtension(MultipartFile multipartFile) throws Exception {
        byte[] header = new byte[12];
        int read;
        try (InputStream in = multipartFile.getInputStream()) {
            read = in.read(header);
        }
        if (read < 4) {
            throw new IllegalArgumentException("图片文件格式不正确");
        }
        String hex = Hex.encodeHexString(header);
        if (hex.startsWith("ffd8ff")) {
            return "jpg";
        }
        if (hex.startsWith("89504e470d0a1a0a")) {
            return "png";
        }
        if (hex.startsWith("47494638")) {
            return "gif";
        }
        throw new IllegalArgumentException("仅支持jpg/png/gif图片上传");
    }

    /**
     * 文档文件头识别：
     * - pdf: 25 50 44 46
     * - docx/xlsx/pptx: 50 4B 03 04 (ZIP格式，需进一步校验内部结构)
     */
    private String detectDocumentExtension(MultipartFile multipartFile, String originalExt) throws Exception {
        byte[] header = new byte[8];
        int read;
        try (InputStream in = multipartFile.getInputStream()) {
            read = in.read(header);
        }
        if (read < 4) {
            throw new IllegalArgumentException("文档文件格式不正确");
        }
        String hex = Hex.encodeHexString(header);

        // PDF文件
        if (hex.startsWith("25504446")) {
            if (!"pdf".equalsIgnoreCase(originalExt)) {
                throw new IllegalArgumentException("文件实际类型为PDF，与扩展名不符");
            }
            return "pdf";
        }

        // Office 2007+格式（基于ZIP）
        if (hex.startsWith("504b0304")) {
            if ("docx".equalsIgnoreCase(originalExt)) {
                return "docx";
            } else if ("xlsx".equalsIgnoreCase(originalExt)) {
                return "xlsx";
            } else if ("pptx".equalsIgnoreCase(originalExt)) {
                return "pptx";
            } else {
                throw new IllegalArgumentException("不支持的Office文档类型");
            }
        }

        throw new IllegalArgumentException("不支持的文档格式");
    }


    public interface UploadFileNameHandle {
        /**
         * 回调处理接口
         * @param originalFilename
         * @return
         */
        String handle(String originalFilename);
    }

    public class DefaultUploadFileNameHandleImpl implements UploadFileNameHandle {

        @Override
        public String handle(String fileExtension) {
            // 这里可自定义文件名称，比如按照业务类型/文件格式/日期
            // 此处按照文件日期存储
            String dateString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            String uniqueSuffix = IdUtil.simpleUUID();
            return dateString + "_" + uniqueSuffix + "." + StringUtils.lowerCase(StringUtils.trim(fileExtension));
        }
    }
}
