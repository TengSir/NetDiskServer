package edu.hbuas.netdisk.config;

/**
 * 网盘配置类
 * @author tengsir
 *
 */
public interface NetDiskConfig {
	public String netDiskServerIP="localhost";
	public String databaseServerIP=netDiskServerIP;
	public String jdbcDriverClass="com.mysql.jdbc.Driver";
	public String jdbcURL="jdbc:mysql://"+databaseServerIP+":3306/netdisk?useSSL=false";
	public String jdbcUsername="root";
	public String jdbcPassword="root";
	public int netDiskServerPort=9999;
	public String serverStoreFileBasePath="/Users/tengsir/newdiskserver/";

}
