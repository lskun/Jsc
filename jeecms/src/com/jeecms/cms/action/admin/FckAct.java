package com.jeecms.cms.action.admin;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.jeecms.cms.entity.main.CmsSite;
import com.jeecms.cms.entity.main.MarkConfig;
import com.jeecms.cms.web.CmsUtils;
import com.jeecms.common.fck.Command;
import com.jeecms.common.fck.ResourceType;
import com.jeecms.common.fck.UploadResponse;
import com.jeecms.common.fck.Utils;
import com.jeecms.common.image.ImageScale;
import com.jeecms.common.image.ImageUtils;
import com.jeecms.common.upload.FileRepository;
import com.jeecms.common.upload.UploadUtils;
import com.jeecms.common.web.springmvc.RealPathResolver;
import com.jeecms.core.entity.Ftp;
import com.jeecms.core.manager.DbFileMng;

/**
 * FCK服务器端接口
 * 
 * 为了更好、更灵活的处理fck上传，重新实现FCK服务器端接口。
 * 
 * @author liufang
 * 
 */
@Controller
public class FckAct {

	private static final Logger log = LoggerFactory.getLogger(FckAct.class);

	// TODO 浏览服务器文件未实现
	// @RequestMapping(value = "/fck/connector.do")
	// public String connector() {
	// return null;
	// }

	@RequestMapping(value = "/fck/upload.do", method = RequestMethod.POST)
	public void upload(
			@RequestParam(value = "Command", required = false) String commandStr,
			@RequestParam(value = "Type", required = false) String typeStr,
			@RequestParam(value = "CurrentFolder", required = false) String currentFolderStr,
			@RequestParam(value = "mark", required = false) Boolean mark,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		log.debug("Entering Dispatcher#doPost");
		responseInit(response);
		if (Utils.isEmpty(commandStr) && Utils.isEmpty(currentFolderStr)) {
			commandStr = "QuickUpload";
			currentFolderStr = "/";
			if (Utils.isEmpty(typeStr)) {
				typeStr = "File";
			}
		}
		if (currentFolderStr != null && !currentFolderStr.startsWith("/")) {
			currentFolderStr = "/".concat(currentFolderStr);
		}
		log.debug("Parameter Command: {}", commandStr);
		log.debug("Parameter Type: {}", typeStr);
		log.debug("Parameter CurrentFolder: {}", currentFolderStr);
		UploadResponse ur = validateUpload(request, commandStr, typeStr,
				currentFolderStr);
		if (ur == null) {
			ur = doUpload(request, typeStr, currentFolderStr, mark);
		}
		PrintWriter out = response.getWriter();
		out.print(ur);
		out.flush();
		out.close();
	}

	private UploadResponse doUpload(HttpServletRequest request, String typeStr,
			String currentFolderStr, Boolean mark) throws Exception {
		ResourceType type = ResourceType.getDefaultResourceType(typeStr);
		try {
			MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
			// We upload just one file at the same time
			MultipartFile uplFile = multipartRequest.getFileMap().entrySet()
					.iterator().next().getValue();
			// Some browsers transfer the entire source path not just the
			// filename
			String filename = FilenameUtils.getName(uplFile
					.getOriginalFilename());
			log.debug("Parameter NewFile: {}", filename);
			String ext = FilenameUtils.getExtension(filename);
			if (type.isDeniedExtension(ext)) {
				return UploadResponse.getInvalidFileTypeError(request);
			}
			if (type.equals(ResourceType.IMAGE)
					&& !ImageUtils.isImage(uplFile.getInputStream())) {
				return UploadResponse.getInvalidFileTypeError(request);
			}
			String fileUrl;
			CmsSite site = CmsUtils.getSite(request);
			MarkConfig conf = site.getConfig().getMarkConfig();
			if (mark == null) {
				mark = conf.getOn();
			}
			boolean isImg = type.equals(ResourceType.IMAGE);
			if (site.getConfig().getUploadToDb()) {
				if (mark && isImg) {
					File tempFile = mark(uplFile, conf);
					fileUrl = dbFileMng.storeByExt(site.getUploadPath(), ext,
							new FileInputStream(tempFile));
					tempFile.delete();
				} else {
					fileUrl = dbFileMng.storeByExt(site.getUploadPath(), ext,
							uplFile.getInputStream());
				}
				// 加上访问地址
				String dbFilePath = site.getConfig().getDbFileUri();
				fileUrl = request.getContextPath() + dbFilePath + fileUrl;
			} else if (site.getUploadFtp() != null) {
				Ftp ftp = site.getUploadFtp();
				if (mark && isImg) {
					File tempFile = mark(uplFile, conf);
					fileUrl = ftp.storeByExt(site.getUploadPath(), ext,
							new FileInputStream(tempFile));
					tempFile.delete();
				} else {
					fileUrl = ftp.storeByExt(site.getUploadPath(), ext, uplFile
							.getInputStream());
				}
				// 加上url前缀
				fileUrl = ftp.getUrl() + fileUrl;
			} else {
				if (mark && isImg) {
					File tempFile = mark(uplFile, conf);
					fileUrl = fileRepository.storeByExt(site.getUploadPath(),
							ext, tempFile);
					tempFile.delete();
				} else {
					fileUrl = fileRepository.storeByExt(site.getUploadPath(),
							ext, uplFile);
				}
				// 加上部署路径
				fileUrl = request.getContextPath() + fileUrl;
			}
			return UploadResponse.getOK(request, fileUrl);
		} catch (IOException e) {
			return UploadResponse.getFileUploadWriteError(request);
		}
	}

	private void responseInit(HttpServletResponse response) {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
	}

	private UploadResponse validateUpload(HttpServletRequest request,
			String commandStr, String typeStr, String currentFolderStr) {
		// TODO 是否允许上传
		if (!Command.isValidForPost(commandStr)) {
			return UploadResponse.getInvalidCommandError(request);
		}
		if (!ResourceType.isValidType(typeStr)) {
			return UploadResponse.getInvalidResourceTypeError(request);
		}
		if (!UploadUtils.isValidPath(currentFolderStr)) {
			return UploadResponse.getInvalidCurrentFolderError(request);
		}
		return null;
	}

	private File mark(MultipartFile file, MarkConfig conf) throws Exception {
		String path = System.getProperty("java.io.tmpdir");
		File tempFile = new File(path, String.valueOf(System
				.currentTimeMillis()));
		file.transferTo(tempFile);
		boolean imgMark = !StringUtils.isBlank(conf.getImagePath());
		if (imgMark) {
			File markImg = new File(realPathResolver.get(conf.getImagePath()));
			imageScale.imageMark(tempFile, tempFile, conf.getMinWidth(), conf
					.getMinHeight(), conf.getPos(), conf.getOffsetX(), conf
					.getOffsetY(), markImg);
		} else {
			imageScale.imageMark(tempFile, tempFile, conf.getMinWidth(), conf
					.getMinHeight(), conf.getPos(), conf.getOffsetX(), conf
					.getOffsetY(), conf.getContent(), Color.decode(conf
					.getColor()), conf.getSize(), conf.getAlpha());
		}
		return tempFile;
	}

	private FileRepository fileRepository;
	private DbFileMng dbFileMng;
	private ImageScale imageScale;
	private RealPathResolver realPathResolver;

	@Autowired
	public void setFileRepository(FileRepository fileRepository) {
		this.fileRepository = fileRepository;
	}

	@Autowired
	public void setDbFileMng(DbFileMng dbFileMng) {
		this.dbFileMng = dbFileMng;
	}

	@Autowired
	public void setImageScale(ImageScale imageScale) {
		this.imageScale = imageScale;
	}

	@Autowired
	public void setRealPathResolver(RealPathResolver realPathResolver) {
		this.realPathResolver = realPathResolver;
	}

}
