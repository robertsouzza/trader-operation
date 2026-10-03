package com.traderoperation.shared.storage;

import com.traderoperation.shared.config.TraderProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Acesso ao bucket de arquivos. Os módulos de negócio declaram o próprio port de saída
 * e o implementam delegando para cá.
 */
@Component
public class S3StorageAdapter {

    private final S3Client s3;
    private final String bucket;

    public S3StorageAdapter(S3Client s3, TraderProperties props) {
        this.s3 = s3;
        this.bucket = props.s3().bucket();
    }

    public void salvar(String chave, byte[] conteudo, String contentType) {
        s3.putObject(b -> b.bucket(bucket).key(chave).contentType(contentType), RequestBody.fromBytes(conteudo));
    }

    public byte[] ler(String chave) {
        return s3.getObjectAsBytes(b -> b.bucket(bucket).key(chave)).asByteArray();
    }

    public void remover(String chave) {
        s3.deleteObject(b -> b.bucket(bucket).key(chave));
    }
}
