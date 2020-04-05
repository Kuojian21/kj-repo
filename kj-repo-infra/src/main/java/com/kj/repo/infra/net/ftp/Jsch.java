package com.kj.repo.infra.net.ftp;

import java.util.Properties;

import org.apache.commons.pool2.impl.GenericObjectPool;

import com.google.common.base.Strings;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.kj.repo.infra.base.function.Consumer;
import com.kj.repo.infra.base.function.Function;
import com.kj.repo.infra.base.pool.Pool;
import com.kj.repo.infra.helper.GenericPoolHelper;

/**
 * @author kj
 */
public class Jsch extends Pool<ChannelSftp> {


    public Jsch(final GenericObjectPool<ChannelSftp> pool) {
        super(pool);
    }

    public static ChannelSftp sftp(String host, int port, String username, String password) throws JSchException {
        JSch jsch = new JSch();
        Session sshSession = jsch.getSession(username, host, port);
        sshSession.setPassword(password);
        Properties sshConfig = new Properties();
        sshConfig.put("StrictHostKeyChecking", "no");
        sshSession.setConfig(sshConfig);
        sshSession.connect();
        Channel channel = sshSession.openChannel("sftp");
        channel.connect();
        return (ChannelSftp) channel;
    }

    public static ChannelSftp sftp(String host, int port, String username, String prvfile, String pubfile,
            byte[] passphrase) throws JSchException {
        JSch jsch = new JSch();
        jsch.addIdentity(prvfile, pubfile, passphrase);
        Session sshSession = jsch.getSession(username, host, port);
        Properties sshConfig = new Properties();
        sshConfig.put("StrictHostKeyChecking", "no");
        sshSession.setConfig(sshConfig);
        sshSession.connect();
        Channel channel = sshSession.openChannel("sftp");
        channel.connect();
        return (ChannelSftp) channel;
    }

    public static Jsch jsch(String host, int port, String username, String password) {
        return new Jsch(
                GenericPoolHelper.wrap(() -> sftp(host, port, username, password), obj -> {
                    obj.getSession().disconnect();
                    obj.disconnect();
                }));
    }

    public static Jsch jsch(String host, int port, String username, String prvfile, String pubfile,
            byte[] passphrase) {
        return new Jsch(
                GenericPoolHelper.wrap(() -> sftp(host, port, username, prvfile, pubfile, passphrase), obj -> {
                    obj.getSession().disconnect();
                    obj.disconnect();
                }));
    }

    public void doExecute(Consumer<ChannelSftp> consumer) throws Exception {
        super.execute(sftp -> {
            String root = null;
            try {
                root = sftp.pwd();
                consumer.accept(sftp);
            } finally {
                if (Strings.isNullOrEmpty(root)) {
                    sftp.cd(root);
                }
            }
        });
    }

    public final <R> R doExecute(Function<ChannelSftp, R> function) throws Exception {
        return super.execute(sftp -> {
            String root = null;
            try {
                root = sftp.pwd();
                return function.apply(sftp);
            } finally {
                if (Strings.isNullOrEmpty(root)) {
                    sftp.cd(root);
                }
            }
        });
    }
}
