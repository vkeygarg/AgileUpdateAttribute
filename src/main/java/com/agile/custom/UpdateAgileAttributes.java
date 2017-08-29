package com.agile.custom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.agile.api.APIException;
import com.agile.api.AgileSessionFactory;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.ICommodity;
import com.agile.api.ICustomer;
import com.agile.api.IDataObject;
import com.agile.api.IDeclaration;
import com.agile.api.IDiscussion;
import com.agile.api.IFileFolder;
import com.agile.api.IFolder;
import com.agile.api.IItem;
import com.agile.api.IManufacturer;
import com.agile.api.IManufacturerPart;
import com.agile.api.IManufacturingSite;
import com.agile.api.IPackage;
import com.agile.api.IPrice;
import com.agile.api.IProgram;
import com.agile.api.IProject;
import com.agile.api.IQualityChangeRequest;
import com.agile.api.IQuery;
import com.agile.api.IRequestForQuote;
import com.agile.api.IServiceRequest;
import com.agile.api.ISpecification;
import com.agile.api.ISubstance;
import com.agile.api.ISupplier;
import com.agile.api.ISupplierResponse;
import com.agile.api.ITransferOrder;
import com.agile.api.IUser;
import com.agile.api.IUserGroup;
import com.agile.api.ManufacturerPartConstants;

public class UpdateAgileAttributes {
	IAgileSession aglSession = null;
	static Logger logger = Logger.getLogger(UpdateAgileAttributes.class);
	Properties prop = null;
	String[] apiList;
	String srcFilePath;
	String delimiter = ",";
	int aglObjClass = IItem.OBJECT_TYPE;
	int totalrecords = 0;
	int failedRecord = 0;
	int passedRecord = 0;
	Map<String, String> failMap = new HashMap<String, String>();

	public void init() throws IOException, APIException {
		loadPropertyFile();
		setAgileSession();
		srcFilePath = prop.getProperty("INPUT_FILE_PATH");
		if (prop.getProperty("DELIMITER") != null)
			delimiter = prop.getProperty("DELIMITER");
		if (prop.getProperty("AGILE_OBJECT") != null)
			setObjectType(prop.getProperty("AGILE_OBJECT"));
	}

	private void setObjectType(String stype) {
		if ("change".equalsIgnoreCase(stype))
			aglObjClass = IChange.OBJECT_TYPE;
		else if ("commodity".equalsIgnoreCase(stype))
			aglObjClass = ICommodity.OBJECT_TYPE;
		else if ("customer".equalsIgnoreCase(stype))
			aglObjClass = ICustomer.OBJECT_TYPE;
		else if ("declaration".equalsIgnoreCase(stype))
			aglObjClass = IDeclaration.OBJECT_TYPE;
		else if ("discussion".equalsIgnoreCase(stype))
			aglObjClass = IDiscussion.OBJECT_TYPE;
		else if ("filefolder".equalsIgnoreCase(stype))
			aglObjClass = IFileFolder.OBJECT_TYPE;
		else if ("folder".equalsIgnoreCase(stype))
			aglObjClass = IFolder.OBJECT_TYPE;
		else if ("item".equalsIgnoreCase(stype))
			aglObjClass = IItem.OBJECT_TYPE;
		else if ("manufacturer".equalsIgnoreCase(stype))
			aglObjClass = IManufacturer.OBJECT_TYPE;
		else if ("manufacturerpart".equalsIgnoreCase(stype))
			aglObjClass = IManufacturerPart.OBJECT_TYPE;
		else if ("manufacturingsite".equalsIgnoreCase(stype))
			aglObjClass = IManufacturingSite.OBJECT_TYPE;
		else if ("package".equalsIgnoreCase(stype))
			aglObjClass = IPackage.OBJECT_TYPE;
		else if ("price".equalsIgnoreCase(stype))
			aglObjClass = IPrice.OBJECT_TYPE;
		else if ("program".equalsIgnoreCase(stype))
			aglObjClass = IProgram.OBJECT_TYPE;
		else if ("project".equalsIgnoreCase(stype))
			aglObjClass = IProject.OBJECT_TYPE;
		else if ("qualitychangerequest".equalsIgnoreCase(stype))
			aglObjClass = IQualityChangeRequest.OBJECT_TYPE;
		else if ("query".equalsIgnoreCase(stype))
			aglObjClass = IQuery.OBJECT_TYPE;
		else if ("requestforquote".equalsIgnoreCase(stype))
			aglObjClass = IRequestForQuote.OBJECT_TYPE;
		else if ("servicerequest".equalsIgnoreCase(stype))
			aglObjClass = IServiceRequest.OBJECT_TYPE;
		else if ("specification".equalsIgnoreCase(stype))
			aglObjClass = ISpecification.OBJECT_TYPE;
		else if ("substance".equalsIgnoreCase(stype))
			aglObjClass = ISubstance.OBJECT_TYPE;
		else if ("supplier".equalsIgnoreCase(stype))
			aglObjClass = ISupplier.OBJECT_TYPE;
		else if ("supplierresponse".equalsIgnoreCase(stype))
			aglObjClass = ISupplierResponse.OBJECT_TYPE;
		else if ("transferorder".equalsIgnoreCase(stype))
			aglObjClass = ITransferOrder.OBJECT_TYPE;
		else if ("user".equalsIgnoreCase(stype))
			aglObjClass = IUser.OBJECT_TYPE;
		else if ("usergroup".equalsIgnoreCase(stype))
			aglObjClass = IUserGroup.OBJECT_TYPE;
		else
			aglObjClass = IItem.OBJECT_TYPE;

	}

	public void loadPropertyFile() throws IOException {
		prop = new Properties();
		FileInputStream file = null;
		String propFileName = "config.properties";
		try {
			file = new FileInputStream(propFileName);
			prop.load(file);
			PropertyConfigurator.configure(prop);
			logger.trace("config File initialized");
		} catch (IOException e) {
			logger.error(e.getMessage());
			throw e;
		} finally {
			if (file != null)
				file.close();
		}

	}

	public static void main(String[] args) {
		UpdateAgileAttributes a = new UpdateAgileAttributes();
		try {
			a.init();
			logger.info("Start.."+new Date());
			a.updateAttr();
			logger.info("Complete.."+new Date());
			if (a.aglSession != null)
				a.aglSession.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateAttr() {
		File srcFile = new File(srcFilePath);
		BufferedReader br = null;
		Reader in = null;
		if (srcFile.exists() && srcFile.isFile()) {
			try {
				in = new FileReader(srcFile);
				br = new BufferedReader(in);
				apiList = br.readLine().split(delimiter);
				String dataList = br.readLine();
				while (dataList != null) {
					++totalrecords;
					updateAttributes(dataList.split(delimiter));
					dataList = br.readLine();
				}
			} catch (IOException e) {
				logger.error(srcFilePath + " -- \t\t" + e.getMessage(), e);
			} finally {
				try {
					if (br != null)
						br.close();
					if (in != null)
						in.close();
				} catch (IOException e) {
					logger.error(srcFilePath + " -- \t\t" + e.getMessage(), e);
				}
			}
			logger.trace(srcFile.getName() + "processed successfully.");
			logger.info("Total Records : " + totalrecords);
			logger.info("Processed Successfully : " + passedRecord);
			logger.info("Failed to Process : " + failedRecord);
			printFailMap();
		} else {
			logger.error(srcFilePath + " is not availabe/valid.");
		}
	}

	private void printFailMap() {
		if (!failMap.isEmpty()) {
			StringBuilder fmsg = new StringBuilder(System.getProperty("line.separator"))
					.append("\nFailed Records with reason:");
			for (Entry<String, String> ent : failMap.entrySet()) {
				fmsg.append(System.getProperty("line.separator")).append("\t").append(ent.getKey()).append(": ")
						.append(ent.getValue());
			}
			logger.info(fmsg);
		}
	}

	private void updateAttributes(String[] dataList) {
		IDataObject aglObj = null;
		int dataIdx = 1;
		try {
			if (aglObjClass == IManufacturerPart.OBJECT_TYPE) {
				Map<Integer, String> params = new HashMap<Integer, String>();
				params.put(ManufacturerPartConstants.ATT_GENERAL_INFO_MANUFACTURER_NAME, dataList[0]);
				params.put(ManufacturerPartConstants.ATT_GENERAL_INFO_MANUFACTURER_PART_NUMBER, dataList[1]);
				aglObj = (IDataObject) aglSession.getObject(IManufacturerPart.OBJECT_TYPE, params);
				dataIdx = 2;
			} else
				aglObj = (IDataObject) aglSession.getObject(aglObjClass, dataList[0]);
			if (aglObj != null) {
				Map<Object, Object> paramMap = new HashMap<Object, Object>();
				for (int j = dataIdx; j < apiList.length; j++) {
					try {
						paramMap.put(Integer.parseInt(apiList[j]), dataList[j]);
					} catch (NumberFormatException e) {
						paramMap.put(apiList[j], dataList[j]);
					}
				}
				aglObj.setValues(paramMap);
				passedRecord++;
			}
		} catch (APIException e) {
			failedRecord++;
			failMap.put(dataList[0], e.getMessage());
			logger.debug(dataList[0] + "\t" + e.getMessage(), e);
		}

	}

	public void setAgileSession() throws APIException {
		HashMap<Integer, String> params = new HashMap<Integer, String>();
		params.put(AgileSessionFactory.USERNAME, prop.getProperty("AGILE_USER_NAME"));
		params.put(AgileSessionFactory.PASSWORD, prop.getProperty("AGILE_PASSWORD"));
		params.put(AgileSessionFactory.URL, prop.getProperty("AGILE_URL"));
		aglSession = AgileSessionFactory.createSessionEx(params);
		logger.trace("Connected to Agile!!!");
	}

}
