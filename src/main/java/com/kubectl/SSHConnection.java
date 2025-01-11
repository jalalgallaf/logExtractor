package com.kubectl;

import com.jcraft.jsch.*;
import java.io.*;
import java.util.function.Consumer;

public class SSHConnection {
    private Session session;
    private Channel watchChannel;
    private volatile boolean isWatching;

    public boolean connect(String host, String username, String password) {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            return true;
        } catch (JSchException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    public String executeCommand(String command) throws JSchException, IOException {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("bash -l -c '" + command.replace("'", "'\\''") + "'");

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.setErrStream(errorStream);

            channel.connect();

            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    break;
                }
            }

            String response = responseStream.toString();
            String error = errorStream.toString();

            if (!error.isEmpty()) {
                throw new IOException(error);
            }

            return response;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public void startWatchingLogs(String command, Consumer<String> outputConsumer) throws JSchException, IOException {
        if (isWatching) {
            stopWatchingLogs();
        }

        watchChannel = session.openChannel("exec");
        ((ChannelExec) watchChannel).setCommand("bash -l -c '" + command.replace("'", "'\\''") + "'");

        InputStream in = watchChannel.getInputStream();
        watchChannel.connect();
        isWatching = true;

        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while (isWatching && (line = reader.readLine()) != null) {
                    outputConsumer.accept(line + "\n");
                }
            } catch (IOException e) {
                if (isWatching) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stopWatchingLogs() {
        isWatching = false;
        if (watchChannel != null) {
            watchChannel.disconnect();
            watchChannel = null;
        }
    }

    public void disconnect() {
        if (session != null) {
            session.disconnect();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        disconnect();
        super.finalize();
    }
}