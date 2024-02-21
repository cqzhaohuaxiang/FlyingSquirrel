/** （最好以系统驱动的方式来实现 总感觉JAVA 的Robot类来处理有些功能无法实现） 有时间重写以驱动的方式运行
 *  
 * 
 * win10 好多时候会出现多网卡（加入组播时所有网卡都加）
 * 
 *  在防火墙中开启相应的服务端口 （在此TCP 与 UDP 的用 36870 端口  ） 
 * 
 * 
 * 模拟输入（键盘与鼠标） 采用JAVA的Robot类  无法实现USB-HID设备的全部功能（比如长按 Ctrl + Alt + Del 组合键）
 * 
 * 
*/

import java.io.*;
import java.net.*;
// import java.nio.file.DirectoryStream.Filter;
import java.util.Enumeration;
import java.util.concurrent.LinkedBlockingQueue;

public class FlyingSquirrelServer {
	static int port = 36870;
	static ServerSocket serverSocket = null;
	static MulticastSocket	multicastSocket = null;


	/**程序入口*/
	public static void main(String[] args) {
		udpReportingSocket();
		
	}

	/**
	 * 用途：定时向网络中广播数据，好让客户端知道自己的IP地址后进行连接
	*/
	private static void  udpReportingSocket() {
		new Thread(new Runnable() {
    		@Override
       		public void run() {
				System.out.println("组播数据");
				try {				
					String getLocalIP = null;
					getLocalIP = InetAddress.getLocalHost().getHostAddress(); //取本机ip地址
					System.out.println("本机地址"+getLocalIP);


					if(getLocalIP != null){
						InetAddress  group = InetAddress.getByName("232.10.11.12");//组播地址
						multicastSocket = new MulticastSocket(port); 
						multicastSocket.setLoopbackMode(true);//禁用组播环回
						// multicastSocket.joinGroup(group);//加入到组播组
						multicastSocket.setSoTimeout(255);//数据报经过的路由跳数
					
						// 获得本机的所有网络接口 都加入到组播中去
						Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
						while (nifs.hasMoreElements()) {
							NetworkInterface nif = nifs.nextElement();
							// 获得与该网络接口绑定的 IP 地址，一般只有一个
							Enumeration<InetAddress> addresses = nif.getInetAddresses();
							while (addresses.hasMoreElements()) {
							InetAddress addr = addresses.nextElement();
								// 我们只关心 IPv4 地址
								if (addr instanceof Inet4Address) { 
									// System.out.println("网卡接口名称：" + nif.getName());
									// System.out.println("网卡接口地址：" + addr.getHostAddress());
									//加入到组播组
									multicastSocket.joinGroup(new InetSocketAddress(group,port),nif);
								}
							}
						}
						
						String message = "I am a keyboard and mouse service";
                        byte[] buffer = message.getBytes();


                        tcpReportingSocket();//启用Socket服务 

						while (true) {
							DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, group, port);
                            multicastSocket.send(datagramPacket);//发送组播数据报
							
                            Thread.sleep(300);
						}

					}
				} catch (UnknownHostException e ) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}		
       		}
   		}).start();
	  
	}


	/*接收从客户端发过的USB-HID 报告符数据*/
	private static void tcpReportingSocket(){

		new Thread(new Runnable() {
    		@Override
       		public void run() {
				try {
					serverSocket = new ServerSocket(port);
					// serverSocket.setSoTimeout(1000);                         
					/**数据队列来平衡速度*/
					LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>();
					
					new QueueThread(queue).start();

					while (true) {
						System.out.println("...等待连接...");
						Socket socket = serverSocket.accept();
						
						InetAddress address = socket.getInetAddress();
						System.out.println("来了一位客人 "+address.getHostAddress());
						/**新开线程来处理连接数据*/
						new SocketServerThread(socket,queue).start();
	

					}

				} catch (IOException e) {
					e.printStackTrace();
				}		

			}
		}).start();
		
	}






}





