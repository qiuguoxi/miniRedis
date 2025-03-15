package com.qgx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// 简易版 Redis 服务器，支持 PING、ECHO、SET、GET 命令，使用 I/O 多路复用
public class Main {
    // 服务器监听的端口
    private static final int PORT = 6379;
    private static Data data = new Data();
    // 主方法
    //抑制警告
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        // 创建 ServerSocketChannel 和 Selector
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()) {

            // 配置服务器通道
            serverSocketChannel.bind(new InetSocketAddress(PORT)); // 绑定端口
            serverSocketChannel.configureBlocking(false); // 设置为非阻塞模式
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); // 注册 ACCEPT 事件

            Task.rdbStart(data);
            System.out.println("Mini Redis Server started on port " + PORT);

            // 事件循环
            while (true) {
                selector.select(); // 阻塞等待事件
                Set<SelectionKey> selectedKeys = selector.selectedKeys(); // 获取就绪的事件集合
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                // 遍历所有就绪的事件
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove(); // 移除已处理的事件

                    if (key.isAcceptable()) {
                        // 处理新连接
                        handleAccept(serverSocketChannel, selector);
                    } else if (key.isReadable()) {
                        // 处理客户端请求
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 处理新连接
    private static void handleAccept(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        // 接受客户端连接
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false); // 设置为非阻塞模式
        clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024)); // 注册 READ 事件，并分配缓冲区
        System.out.println("New client connected: " + clientChannel.getRemoteAddress());
    }

    // 处理客户端请求
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel(); // 获取客户端通道
        ByteBuffer buffer = (ByteBuffer) key.attachment(); // 获取关联的缓冲区

        try {
            // 读取客户端数据
            int bytesRead = clientChannel.read(buffer);
            if (bytesRead == -1) {
                // 客户端关闭连接
                System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
                clientChannel.close();
                return;
            }

            buffer.flip(); // 切换到读模式
            String request = new String(buffer.array(), 0, buffer.limit()).trim(); // 将缓冲区数据转换为字符串
            buffer.clear(); // 清空缓冲区，准备下一次读取

            // 解析 RESP 协议命令
            String response = parseAndHandleCommand(request);
            if (response != null) {
                // 发送响应
                ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
                clientChannel.write(responseBuffer);
            }
        } catch (IOException e) {
            // 客户端异常断开
            System.out.println("Client disconnected abruptly: " + clientChannel.getRemoteAddress());
            clientChannel.close();
        }
    }


    // 解析并执行 Redis 命令
    private static String parseAndHandleCommand(String request) {
        // 按行分割请求数据
        String[] lines = request.split("\r\n");
        if (lines.length < 1 || !lines[0].startsWith("*")) {
            return "-ERR invalid command\r\n"; // 返回错误信息
        }

        // 获取命令参数数量
        int numArgs = Integer.parseInt(lines[0].substring(1));
        List<String> args = new ArrayList<>();

        // 解析命令参数
        for (int i = 1; i < lines.length; i += 2) {
            if (lines[i].startsWith("$")) {
                args.add(lines[i + 1]);
            }
        }

        // 检查参数数量是否匹配
        if (args.size() != numArgs) {
            return "-ERR invalid command format\r\n"; // 返回错误信息
        }

        // 处理命令并返回结果
        return Command.handleCommand(args,data);
    }

    // 处理 Redis 命令

}