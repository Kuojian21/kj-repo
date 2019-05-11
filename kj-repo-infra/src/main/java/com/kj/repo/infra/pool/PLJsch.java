package com.kj.repo.infra.pool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.google.common.base.Strings;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * @param <T>
 * @author kuojian21
 */
public class PLJsch<T> {

    private final GenericObjectPool<T> pool;

    public PLJsch(final GenericObjectPool<T> pool) {
        this.pool = pool;
    }

    public final <R> R execute(Function<T, R> function) throws Exception {
        T t = null;
        try {
            t = this.pool.borrowObject();
            return function.apply(t);
        } finally {
            if (t != null) {
                this.pool.returnObject(t);
            }
        }
    }

    public final void execute(Consumer<T> consumer) throws Exception {
        T t = null;
        try {
            t = this.pool.borrowObject();
            consumer.accept(t);
        } finally {
            if (t != null) {
                this.pool.returnObject(t);
            }
        }
    }

    /**
     * @author kuojian21
     */
    @FunctionalInterface
    public static interface Function<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * @author kuojian21
     */
    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t) throws Exception;
    }

    /**
     * @author kuojian21
     */
    public static class Jsch {
        public static PLJsch<ChannelSftp> jsch(String host, int port, String username, String password) {
            return new PLJsch<ChannelSftp>(
                    new GenericObjectPool<ChannelSftp>(new BasePooledObjectFactory<ChannelSftp>() {
                        @Override
                        public ChannelSftp create() throws Exception {
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

                        @Override
                        public PooledObject<ChannelSftp> wrap(ChannelSftp obj) {
                            return new DefaultPooledObject<ChannelSftp>(obj);
                        }

                        @Override
                        public void destroyObject(final PooledObject<ChannelSftp> obj)
                                throws Exception {
                            obj.getObject().disconnect();
                            obj.getObject().getSession().disconnect();
                        }
                    }));
        }

        public static PLJsch<ChannelSftp> jsch(String host, int port, String username, String prvfile, String pubfile,
                                               byte[] passphrase) {
            return new PLJsch<ChannelSftp>(
                    new GenericObjectPool<ChannelSftp>(new BasePooledObjectFactory<ChannelSftp>() {
                        @Override
                        public ChannelSftp create() throws Exception {
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

                        @Override
                        public PooledObject<ChannelSftp> wrap(ChannelSftp obj) {
                            return new DefaultPooledObject<ChannelSftp>(obj);
                        }

                        @Override
                        public void destroyObject(final PooledObject<ChannelSftp> obj)
                                throws Exception {
                            obj.getObject().disconnect();
                            obj.getObject().getSession().disconnect();
                        }
                    }));
        }
    }

    /**
     * @author kuojian21
     */
    public static class Helper {

        public static boolean upload(ChannelSftp sftp, String directory, String file, InputStream is)
                throws SftpException, IOException {
            String root = null;
            try {
                root = sftp.pwd();
                if (!Strings.isNullOrEmpty(directory)) {
                    String[] dirs = directory.split("/");
                    for (String dir : dirs) {
                        if (!Strings.isNullOrEmpty(dir)) {
                            try {
                                sftp.cd(dir);
                            } catch (SftpException sException) {
                                if (ChannelSftp.SSH_FX_NO_SUCH_FILE == sException.id) {
                                    sftp.mkdir(dir);
                                    sftp.cd(dir);
                                } else {
                                    return false;
                                }
                            }
                        }
                    }
                }
                sftp.put(is, file);
                return true;
            } finally {
                if (Strings.isNullOrEmpty(root)) {
                    try {
                        sftp.cd(root);
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                }

            }
        }

        public static boolean download(ChannelSftp sftp, String directory, String file, OutputStream os)
                throws IOException, SftpException {
            String root = null;
            try {
                root = sftp.pwd();
                if (!Strings.isNullOrEmpty(directory)) {
                    sftp.cd(directory);
                }
                sftp.get(file, os);
                os.flush();
                return true;
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } finally {
                        if (Strings.isNullOrEmpty(root)) {
                            sftp.cd(root);
                        }
                    }
                }

            }
        }

    }

}
