package com.ke.ticketsystemke.storage;
import java.io.*;import java.nio.file.Path;
public interface WarrantyFileStorage{StoredFile store(Long claimId,InputStream input) throws IOException;InputStream open(String key) throws IOException;void deletePermanent(String key) throws IOException;boolean exists(String key);Path resolvedPath(String key);record StoredFile(String key,long sizeBytes,String checksum){}}
