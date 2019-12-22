package com.kj.repo.infra.pool.jsch;

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
import com.kj.repo.infra.pool.PLBase;

/**
 * @param <T>
 * @author kuojian21
 */
public class PLJsch extends PLBase<ChannelSftp> {


    public PLJsch(final GenericObjectPool<ChannelSftp> pool) {
        super(pool);
    }

    public static PLJsch jsch(String host, int port, String username, String password) {
        return new PLJsch(new GenericObjectPool<ChannelSftp>(new BasePooledObjectFactory<ChannelSftp>() {
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
            public void destroyObject(final PooledObject<ChannelSftp> obj) throws Exception {
                obj.getObject().disconnect();
                obj.getObject().getSession().disconnect();
            }
        }));
    }

    public static PLJsch jsch(String host, int port, String username, String prvfile, String pubfile,
                              byte[] passphrase) {
        return new PLJsch(new GenericObjectPool<ChannelSftp>(new BasePooledObjectFactory<ChannelSftp>() {
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
            public void destroyObject(final PooledObject<ChannelSftp> obj) throws Exception {
                obj.getObject().disconnect();
                obj.getObject().getSession().disconnect();
            }
        }));
    }

    public boolean upload(String directory, String file, InputStream is) throws Exception {
        return this.execute(sftp -> {
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
        });

    }

    public boolean download(String directory, String file, OutputStream os) throws Exception {
        return this.execute(sftp -> {
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
        });

    }
}
