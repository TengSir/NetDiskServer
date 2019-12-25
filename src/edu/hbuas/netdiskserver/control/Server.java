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
			System.out.println("------>>>服务器启动成功------>>>");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public Server() {
		try {
			//服务器开启后，使用whie循环不停的接受客户端链接（对外提供不间断服务）
			while(true) { 
			Socket  client=server.accept();//接受一个客户端链接
			System.out.println("++++++["+client.getInetAddress().getHostAddress()+"]"+"链接进来了!++++++");
			//每次链接进来一个客户端，我们单独开启一个线程让这个客户端使用独立多线程分支和服务器做io通讯
			ClientThread  c=new ClientThread(client);//创建封装好的线程类对象
			c.start();//开启线程
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
		private Socket  client;
		
		public ClientThread(Socket  client) {
			this.client=client;
			try {
				this.in=new ObjectInputStream(client.getInputStream());
				this.out=new ObjectOutputStream(client.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 从Message中提取数据，并将文件上传到服务器对应的用户空间的方法
		 * @param message
		 * @param in
		 * @param out
		 */
		public void uploadFileToServer(Message  message,ObjectInputStream  in,ObjectOutputStream  out) {
			try {
				String fileName=message.getFilename();//获取用户要上传的文件名
				File userDir=new File(NetDiskConfig.serverStoreFileBasePath+message.getFromUser());//获取用户服务器上的磁盘文件夹
				if(!userDir.exists())userDir.mkdirs();//如果文件夹不存在，说明用户第一次注册，则新建一个文件夹用来存放该用户的文件
				FileOutputStream  fileOut=new FileOutputStream(userDir.getAbsolutePath()+"/"+fileName);//创建一个文件输出流指向这个新要上传的文件对象
				//下面就是IO代码，从socket的in对象（网络输入流对象）里读取网络另外一方（客户端）
				//发送过来的数据并使用上面的文件输出流把数据写入到服务器的用户空间下的对应文件里
				byte[] bs=new byte[1024];
				int length=-1;
				while((length=in.read(bs))!=-1) {
					fileOut.write(bs,0,length);
					fileOut.flush();
				}
				fileOut.close();//文件写入完毕，关闭文件流
			} catch (Exception e) {
			}
			
		}
		/**
		 * 从Message中提取数据，然后将文件发送给客户端的方法
		 * @param message
		 * @param in
		 * @param out
		 */
		public void downloadFileToClient(Message  message,ObjectInputStream  in,ObjectOutputStream  out) {
			try {
				String fileName=message.getFilename();//获得用户要想下载的文件名
				File file=new File(NetDiskConfig.serverStoreFileBasePath+message.getFromUser()+"/"+fileName);//创建File对象，指向服务器上对应的用户要下载的文件
				FileInputStream  fileIn=new FileInputStream(file);//创建文件输入流指向这个要下载的文件对象
				//下面就是IO代码， 使用文件输入流读取文件的数据，然后使用socket的out（输出流）将文件数据写到网络通道的另外一端（客户端）
				byte[] bs=new byte[1024];
				int length=-1;
				while((length=fileIn.read(bs))!=-1) {
					out.write(bs,0,length);
					out.flush();
				}
				fileIn.close();//文件读写完毕，关闭文件流
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		/**
		 * 从Message中提取信息，然后读取用户在服务器端存储的所有文件的方法
		 * @param message
		 * @param in
		 * @param out
		 */
		public void listFilesOfUser(Message  message,ObjectInputStream  in,ObjectOutputStream  out) {
			try {
				String username=message.getFromUser();//获取当前发起这个操作的用户名
				File userDir=new File(NetDiskConfig.serverStoreFileBasePath+username);//构造一个文件对象用来指向用户的网盘文件夹
				if(!userDir.exists()) {//判断服务器上是否有这个用户的磁盘文件夹，如果没有，说明这是一个新的用户，则给他创建一个新的文件夹
					userDir.mkdirs();
				}
				Set<File> filesSet=new HashSet<File>();//定义一个集合用来存储当前用户的所有文件
				
				//使用Java中的File解析这个文件夹下面的文件系统
				
				File[]  allFiles=userDir.listFiles();//读取当前用户文件夹下面的所有文件
				for(File f:allFiles) {
					filesSet.add(f);//便利一个文件，讲当前文件加入到上面的set集合
				}
				
				//服务器端读取了当前用户的所有文件之后也需要封装一个标准的Message对象（里面应该包含了该用户的所有文件信息）
				//发送给用户，通知用户你的磁盘空间里有哪些文件
				Message  allFilesMessage=new Message();
				allFilesMessage.setAllFiles(filesSet);//讲所有文件的集合对象设置到当前的Message属性里
				
				//数据读取完毕，并且封装好了Message，服务器就应该讲这个消息发送给客户端
				out.writeObject(allFilesMessage);
				out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		/**
		 * 从Message中获得要删除的文件信息，然后执行文件的删除操作，并将删除之后的结果返回给用户
		 * @param message
		 * @param in
		 * @param out
		 */
		public void deletFileOfUser(Message  message,ObjectInputStream  in,ObjectOutputStream  out) {
			try {
				String fileName=message.getFilename();//获取用户想要删除的文件名
				File file=new File(NetDiskConfig.serverStoreFileBasePath+message.getFromUser()+"/"+fileName);//创建File对象，指向服务器上对应的用户要删除的文件
				
				
				//删除操作执行完毕，封装一个消息通知用户删除结果
				Message  deleteResult=new Message();
				if(file.exists()) {
					try {
						file.delete();//执行io的删除操作
						deleteResult.setFromUser("true");//封装一个true给客户端，通知客户端删除成功
					} catch (Exception e) {
						deleteResult.setFromUser("false");//封装一个true给客户端，通知客户端删除成功
					}
				}
				
				//数据读取完毕，并且封装好了Message，服务器就应该讲这个消息发送给客户端
				out.writeObject(deleteResult);
				out.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			/**
			 * 这个run方法是每个客户端的独立线程，用来单独和这个客户端通讯
			 */
			Message  message=null;
			try {
				 message=(Message)in.readObject();
				System.out.println("["+client.getInetAddress().getHostAddress()+"]["+message.getFromUser()+"]客户端发来一个消息:"+message);
				/**
				 * 接到消息后应该判断消息类型，执行不同的操作
				 */
				switch (message.getType()) {
					case UPLOAD:
					{
						System.out.println(">>>客户端执行的是上传操作:>>>");
						//如果是上传消息，则提取上传的文件名等信息然后执行io读写文件并存入服务器用户空间下
						uploadFileToServer(message,in,out);//调用处理上传文件的方法，执行文件上传业务
						System.out.println("["+message.getFromUser()+"]的文件:["+message.getFileLength()+"]上传完毕！");
						break;
					}
					case DOWNLOAD:
					{
						//如果是下载消息，解析出要下载的文件信息，然后执行io操作将文件发送给客户端用户
						    System.out.println("<<<客户端执行的是下载操作:<<<");
						    downloadFileToClient(message,in,out);//调用处理下载文件的方法，执行下载文件业务
							System.out.println("["+message.getFromUser()+"]的文件:["+message.getFilename()+"]下载完毕！");
							
						break;
					}
					case LOADFILES:
					{
						 System.out.println("~~~客户端执行的是读取用户文件列表的操作:~~~");
						//如果是读取用户文件列表则，获取用户名信息然后读取用户对应的文件夹下面的文件列表然后封装成一个标准的Message发送给客户
						 listFilesOfUser(message,in,out);//调用读取文件列表的方法，执行加载文件列表的业务
						System.out.println("["+message.getFromUser()+"]的文件列表读取完毕！");
						break;
					}
					case DELETE:
					{
						 System.out.println("~~~客户端执行的是删除文件操作:~~~");
						//如果是删除消息，解析出消息中要删除的文件信息，然后在服务器端本地执行File的delete操作，删除对应的文件并返回删除的结果给用户
						 deletFileOfUser(message,in,out);//调用读取文件列表的方法，执行加载文件列表的业务
						System.out.println("["+message.getFromUser()+"]的文件删除完毕！");
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				//用户的业务操作完毕，关闭当前socket的流和socket通道（网盘采用短链接，每次业务操作链接一次，操作完毕断开链接）
				try {
					out.close();
					in.close();
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("["+client.getInetAddress().getHostAddress()+"]["+message.getFromUser()+"]客户端操作执行完毕了，socket断开！");
			}
		}
	}
	
	
	public static void main(String[] args) {
		new Server();
	}
}
