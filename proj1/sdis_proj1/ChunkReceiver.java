package sids_proj;

import java.util.HashMap;

// class to receive the file chunks
public class ChunkReceiver {

	// hash map to contain received file chunks
	private HashMap<Integer, FileChunk> chunks = new HashMap<Integer, FileChunk>();
	
	// adds a file chunk to the hash map
	public void addChunk(FileChunk chunk){
		chunks.put(new Integer(chunk.getChunkId()), chunk);
	}
	
	// return the received file chunks as an array of bytes
	public byte[] joinChunks(){
		
		// obtain number of received chunks
		int received = this.chunks.size();
		
		// verify if all chunks were received
		FileChunk lastChunk = this.chunks.get(received - 1);
		if(lastChunk == null)
			return null;
		else if (!lastChunk.isLast())
			return null;
		
		// sort the chunks by id
		int byteCounter = 0;
		FileChunk[] fileChunks = new FileChunk[received];
		for(int id = 0; id < received; id++){
			fileChunks[id] = this.chunks.get(id);
			byteCounter += fileChunks[id].getLength();
		}
		
		// initialize data array
		byte[] data = new byte[byteCounter];
		
		// fill data array
		int bytesToWrite;
		int byteNumber = 0;
		for(int id = 0; id < received; id++){
			bytesToWrite = fileChunks[id].getLength();
			System.arraycopy(fileChunks[id].getContents(), 0, data, byteNumber, bytesToWrite);
			byteNumber += bytesToWrite;
		}
		
		// return result
		return data;
	} 
}
