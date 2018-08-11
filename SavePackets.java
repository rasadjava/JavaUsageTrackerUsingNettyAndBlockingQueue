
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.javausage.tracker.datasource.NettyUsageTracker.IncommingPacketHandler;

public class SavePackets implements Runnable {
	protected BlockingQueue<String> g_queue = null;
	private static final String jdbcDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String jdbcURL = "jdbc:sqlserver://localhost:1433;databaseName=JMT;integratedSecurity=true;";
	// private static final String userName = "UserName";
	// private static final String password = "Password";

	public SavePackets(BlockingQueue<String> g_queue) {
		this.g_queue = g_queue;
	}

	public void savePacket(BlockingQueue<String> queue) {
		System.out.println("Packet received to insertion");
		this.g_queue = queue;
	}

	public void run() {
		System.out.println("run() started..");
		System.out.println("While loop...");
		String str;
		try {
			while ((str = g_queue.take().toString()) != "exit") {
				String excelPacket = str;
				String dbPacket = str;
				System.out.println("!!!!!" + str);
				System.out.println("@@@@@@" + excelPacket);
				System.out.println("@@@@@@" + dbPacket);
				SavePackets.insetPacket(dbPacket);
				SavePackets.generateExcel(excelPacket);
			}
			System.out.println("No Packet in queue : " + str);
		} catch (InterruptedException e) {
			System.out.println("Error Occured");
			e.printStackTrace();
		}
	}

	public static String insetPacket(String packet) {
		System.out.println("Packet ineserting to database.....");
		String status = null;
		String[] splitedPacketContent = packet.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		String TYPE = splitedPacketContent[0].replaceAll("^\"|\"$", "");
		String JAVAINVOCATIONTIME = splitedPacketContent[1].replaceAll("^\"|\"$", "");
		String HOST_IP = splitedPacketContent[2].replaceAll("^\"|\"$", "");
		String APPNAME = splitedPacketContent[3].replaceAll("^\"|\"$", "");
		String JREDIR = splitedPacketContent[4].replaceAll("^\"|\"$", "");
		String JAVAVERS = splitedPacketContent[5].replaceAll("^\"|\"$", "");
		String JVMVERS = splitedPacketContent[6].replaceAll("^\"|\"$", "");
		String JAVAVENDOR = splitedPacketContent[7].replaceAll("^\"|\"$", "");
		String JVMVENDOR = splitedPacketContent[8].replaceAll("^\"|\"$", "");
		String OS = splitedPacketContent[9].replaceAll("^\"|\"$", "");
		String ARCH = splitedPacketContent[10].replaceAll("^\"|\"$", "");
		String OSVERS = splitedPacketContent[11].replaceAll("^\"|\"$", "");
		String JVMARGS = splitedPacketContent[12].replaceAll("^\"|\"$", "");
		String JAVAHOME = splitedPacketContent[13].replaceAll("^\"|\"$", "");
		String USERNAME = splitedPacketContent[14].replaceAll("^\"|\"$", "");
		// Timestamp STOREDATETIME = new Timestamp(date.getTime());
		Connection con = SavePackets.getDBConnection();
		// declare the statement object
		// Statement sqlStatement = null;
		PreparedStatement ps = null;
		try {
			// sqlStatement = con.createStatement();
			ps = con.prepareStatement(
					"insert into JAVA_USAGE_TWO (TYPE, JAVAINVOCATIONTIME, HOST_IP, APPNAME, JREDIR, JAVAVERS, JVMVERS, JAVAVENDOR, JVMVENDOR, OS, ARCH, OSVERS, JVMARGS, JAVAHOME, USERNAME) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		} catch (SQLException e1) {
			status = "Error while creating Statement";
			e1.printStackTrace();
		}
		try {
			ps.setString(1, TYPE);
			ps.setString(2, JAVAINVOCATIONTIME);
			ps.setString(3, HOST_IP);
			ps.setString(4, APPNAME);
			ps.setString(5, JREDIR);
			ps.setString(6, JAVAVERS);
			ps.setString(7, JVMVERS);
			ps.setString(8, JAVAVENDOR);
			ps.setString(9, JVMVENDOR);
			ps.setString(10, OS);
			ps.setString(11, ARCH);
			ps.setString(12, OSVERS);
			ps.setString(13, JVMARGS);
			ps.setString(14, JAVAHOME);
			ps.setString(15, USERNAME);
			// ps.setString(16, STOREDATETIME);
			ps.executeUpdate();
			status = "Data inserted successfully";
		} catch (SQLException e) {
			status = "Error while executing inset query";
			e.printStackTrace();
		}

		System.out.println("Closing database connection");

		// close the database connection
		try {
			con.close();
		} catch (SQLException e) {
			status = "Error while closing connection";
			e.printStackTrace();
		}
		System.out.println("[INFO]: " + status);
		return status;
	}

	public static Connection getDBConnection() {
		Connection databaseConnection = null;
		try {
			Class.forName(jdbcDriver).newInstance();
			// databaseConnection = DriverManager.getConnection(jdbcURL, userName,
			// password);
			databaseConnection = DriverManager.getConnection(jdbcURL);
		} catch (Exception e) {
			System.out.println("[INFO]: Having issue while connection to Database");
			e.printStackTrace();
		}
		return databaseConnection;
	}

	// Store packet into excel sheets logic
	public static void generateExcel(String str) throws IOException {
		boolean isFileExist = false;
		boolean isFilterValueContains = false;
		List<String> filterList = new ArrayList<String>();
		filterList.add("VM start");
		// filterList.add("Java");
		// filterList.add("Audi");
		for (String filterValue : filterList) {
			if (str.contains(filterValue)) {
				isFilterValueContains = true;
			}
		}
		GregorianCalendar date = new GregorianCalendar();
		DateFormatSymbols dfs = new DateFormatSymbols();
		String[] months = dfs.getMonths();
		String fileName = date.get(Calendar.DAY_OF_MONTH) + "_" + months[date.get(Calendar.MONTH)] + "_"
				+ date.get(Calendar.YEAR) + ".csv";
		String filePath = "D:/USAutomation/output_files/";
		List<String> listOfExcelFiles = SavePackets.readAllFileNames(filePath);
		if (isFilterValueContains) {
			fileName = "FilteredList_" + fileName;
		}
		for (String excelFile : listOfExcelFiles) {
			if (fileName.contains(excelFile)) {
				isFileExist = true;
			}
		}
		try {
			if (isFileExist) {
				FileInputStream inputStream = new FileInputStream(new File(filePath + fileName));
				HSSFWorkbook workbook = (HSSFWorkbook) WorkbookFactory.create(inputStream);
				HSSFSheet sheet = workbook.getSheetAt(0);
				int rowCount = sheet.getLastRowNum();
				int columnCount = 0;
				HSSFRow row = sheet.createRow(++rowCount);
				HSSFCell cell = row.createCell(columnCount);
				cell.setCellValue(str);
				inputStream.close();
				FileOutputStream fileOut = new FileOutputStream(filePath + fileName);
				workbook.write(fileOut);
				fileOut.close();
				workbook.close();
			} else {
				HSSFWorkbook workbook = new HSSFWorkbook();
				HSSFSheet sheet = workbook.createSheet("FirstSheet");
				int rowCount = 0;
				HSSFRow row = sheet.createRow(rowCount);
				int shellCount = 0;
				row.createCell(shellCount).setCellValue(str);
				FileOutputStream fileOut = new FileOutputStream(filePath + fileName);
				workbook.write(fileOut);
				fileOut.close();
				workbook.close();

			}
			System.out.println("Your excel file has been generated!");

		} catch (Exception ex) {
			System.out.println(ex);
		}
	}

	protected static List<String> readAllFileNames(String directoryPath) throws IOException {
		List<String> allExcelFiles = new ArrayList<String>();
		File dirFile = new File(directoryPath);
		for (File file : dirFile.listFiles()) {
			String excelFile = file.getName();
			if (excelFile.contains(".csv") || excelFile.contains(".xlsx") && !(excelFile.contains("~$"))) {
				allExcelFiles.add(excelFile);
			}
		}
		return allExcelFiles;
	}

}
