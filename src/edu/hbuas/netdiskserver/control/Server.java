package edu.hbuas.netdiskserver.control;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import edu.hbuas.netdisk.config.NetDiskConfig;
import edu.hbuas.netdisk.model.Message;
/**
 * 封装一个网盘服务器的服务类
 * @author Lenovo
 *
 */
public class Server {
	private ServerSocket server;
	{
		try {
			server=new ServerSocket(9999);
			System.out.println("服务器启动成功");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public Server() {
		try {
			while(true) { 
			Socket  client=server.accept();
			ObjectInputStream in=new ObjectInputStream(client.getInputStream());
			ObjectOutputStream  out=new ObjectOutputStream(client.getOutputStream());
			System.out.println(client.getInetAddress().getHostAddress()+"链接进来了！");
			ClientThread  c=new ClientThread(in, out);
			c.start();//每次链接进来一个客户端，我们单独开启一个线程让这个客户端使用独立多线程分支和服务器做io通讯
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 封装一个内部类，这个内部类主要是开启一个线程和每个客户端独立通讯的类
	 */
	private class  ClientThread extends Thread{
		private ObjectInputStream in;
		private 	ObjectOutputStream  out;
		
		public ClientThread(ObjectInputStream in, ObjectOutputStream out) {
			super();
			this.in = in;
			this.out = out;
		}


		@Override
		public void run() {
			/**
			 * 这个run方法是每个客户端的独立线程，用来单独和这个客户端通讯
			 */
			try {
				Message  message=(Message)in.readObject();
				System.out.println("server recived one message:"+message);
				
				/**
				 * 接到消息后应该判断消息类型，执行不同的操作
				 */
				switch (message.getType()) {
					case UPLOAD:
					{
						String fileName=message.getFilename();
						File userDir=new File(NetDiskConfig.serverStoreFileBasePath+message.getFromUser());
						if(!userDir.exists())userDir.mkdirs();
						FileOutputStream  out=new FileOutputStream(userDir.getAbsolutePath()+"/"+fileName);
						byte[] bs=new byte[1024];
						int length=-1;
						while((length=in.read(bs))!=-1) {
							out.write(bs,0,length);
							out.flush();
						}
						out.close();
						in.close();
						System.out.println(message.getFromUser()+"的文件["+message.getFileLength()+"],上传完毕！");
						
						break;
					}
					case DOWNLOAD:
					{
						System.out.println(message.getFromUser()+"准备下载:"+message.getFilename());
							String fileName=message.getFilename();//获得用户要想下载的文件名
							File file=new File(NetDiskConfig.serverStoreFileBasePath+message.getFromUser()+"/"+fileName);
							FileInputStream  fileIn=new FileInputStream(file);
							byte[] bs=new byte[1024];
							int length=-1;
							while((length=fileIn.read(bs))!=-1) {
								out.write(bs,0,length);
								out.flush();
							}
							System.out.println(message.getFromUser()+"的文件:"+message.getFilename()+"下载完毕");
							
						break;
					}
					case LOADFILES:
					{
						System.out.println(message);
						String username=message.getFromUser();//用户名
						File userDir=new File(NetDiskConfig.serverStoreFileBasePath+username);//构造一个文件对象用来指向用户的网盘文件夹
						if(!userDir.exists()) {//判断服务器上是否有这个用户的磁盘文件夹，如果没有，说明这是一个新的用户，或者该用户删除了所有文件，则给他创建一个新的文件夹
							userDir.mkdirs();
						}
						
					
						Set<File> filesSet=new HashSet<File>();//定义一个集合用来存储当前用户的所有文件
						
						//使用Java中的File解析这个文件夹下面的文件系统
						
						File[]  allFiles=userDir.listFiles();//读取当前用户文件夹下面的所有文件
						for(File f:allFiles) {
							System.out.println(f.getAbsolutePath());
							filesSet.add(f);//便利一个文件，讲当前文件加入到上面的set集合
						}
						
						//服务器端读取了当前用户的所有文件之后也需要封装一个标准的Message对象（里面应该包含了该用户的所有文件信息）
						//发送给用户，通知用户你的磁盘空间里有哪些文件
						Message  allFilesMessage=new Message();
						allFilesMessage.setAllFiles(filesSet);//讲所有文件的集合对象设置到当前的Message属性里
						
						//数据读取完毕，并且封装好了Message，服务器就应该讲这个消息发送给客户端
						out.writeObject(allFilesMessage);
						out.flush();
						System.out.println("文件列表消息回复完毕");
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		}
	}
	
	
	public static void main(String[] args) {
		new Server();
	}
}
