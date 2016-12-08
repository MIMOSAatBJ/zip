package zh.fcsp.cc.zip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;

import org.apache.log4j.Logger;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;


public class Zipper {
	Logger log=Logger.getLogger(getClass());
	private static final int BUFFER = 2048;

	private static final long EMPTY_CRC = new CRC32().getValue();

	private File outputFile = null;

	private File directory = null;

	private FileOutputStream fos = null;

	private ZipOutputStream zos = null;

	private String currentDirName;

	/**
	 * 将directory的内容压缩到outputFile中
	 * @param outputFile
	 * @param directory
	 * @throws IOException
	 */
	public Zipper(File outputFile, File directory) throws IOException {
		this.outputFile = outputFile;
		this.directory = directory;
		currentDirName = directory.getAbsolutePath();
		zip();
	}
	
	/**
	 * 将指定文件夹压缩流输入到指定流中
	 * @param stream
	 * @param directory
	 * @throws Exception
	 */
	public Zipper(OutputStream stream,String file) throws Exception{
		this.directory = new File(file);
		currentDirName = directory.getAbsolutePath();
		getZipStream(stream);
	}

	/**
	 * 将指定的文件/夹，压缩到指定文件中
	 * @throws RuntimeException
	 * @throws IOException
	 */
	public void zip() throws RuntimeException, IOException {
		try {
			fos = new FileOutputStream(outputFile);
			zos = new ZipOutputStream(fos);
			zos.setEncoding("UTF-8");
			zipDir(directory);			
		} finally{
			if(null!=zos){
				zos.flush();
				zos.close();
			}
			if(null!=fos){
				fos.close();
			}
		}
	}
	
	/**
	 * 得到压缩文件流
	 * @return
	 * @throws Exception
	 */
	private void getZipStream(OutputStream stream) throws Exception{
		try {
			zos = new ZipOutputStream(stream);
			zos.setEncoding("UTF-8");
			zipDir(directory);			
		} finally{
			if(null!=zos){
				zos.flush();
				zos.close();
			}
			if(null!=fos){
				fos.close();
			}
		}
	}

	private void zipDir(File dir) throws RuntimeException {
		StringBuffer strBuf = new StringBuffer();
		if (!dir.getPath().equals(currentDirName)) {
			String entryName = dir.getPath().substring(currentDirName.length() + 1);
			entryName = entryName.replace('\\', '/');
			ZipEntry ze = new ZipEntry(entryName + "/");
			if (dir != null && dir.exists()) {
				ze.setTime(dir.lastModified());
			} else {
				ze.setTime(System.currentTimeMillis());
			}
			ze.setSize(0);
			ze.setMethod(ZipEntry.STORED);
			ze.setCrc(EMPTY_CRC);
			try {
				zos.putNextEntry(ze);
			} catch (Exception e) {
				strBuf.append(entryName).append("##");
				log.error("出现异常:"+e.getClass().getSimpleName());
			}
		}
		if (dir.exists() && dir.isDirectory()) {
			File[] fileList = dir.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				if (fileList[i].isDirectory() && this.acceptDir(fileList[i])) {
					zipDir(fileList[i]);
				}
				if (fileList[i].isFile() && this.acceptFile(fileList[i])) {
					try {
						zipFile(fileList[i]);
					} catch (Exception e) {
						strBuf.append(fileList[i].getName()).append("##");
					}
				}
			}
			if(strBuf.length()>0){
				strBuf.append("压缩失败!");
				throw new RuntimeException(strBuf.toString());
			}
		}
	}

	private void zipFile(File file) throws RuntimeException {
		BufferedInputStream bis = null;
		try {
			if (!file.equals(this.outputFile)) {
				bis = new BufferedInputStream(new FileInputStream(file), BUFFER);
				String entryName = file.getPath().substring(currentDirName.length() + 1);
				entryName = entryName.replace('\\', '/');
				ZipEntry fileEntry = new ZipEntry(entryName);
				zos.putNextEntry(fileEntry);
				byte[] data = new byte[BUFFER];
				int byteCount = -1;
				byteCount = bis.read(data, 0, BUFFER);
				while (byteCount != -1) {
					zos.write(data, 0, byteCount);
					byteCount = bis.read(data, 0, BUFFER);
				}
			}			
		} catch (Exception e) {
			log.error("出现异常:"+e.getClass().getSimpleName());
			throw new RuntimeException(file.getName()+"压缩失败!");
		}finally{
			if(bis!=null){
				try {
					bis.close();
				} catch (IOException e) {
					log.error("出现异常:"+e.getClass().getSimpleName());
				}
			}
		}
	}

	protected boolean acceptDir(File dir) {
		return true;
	}

	protected boolean acceptFile(File file) {
		return true;
	}

}