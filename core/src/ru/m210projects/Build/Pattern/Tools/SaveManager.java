package ru.m210projects.Build.Pattern.Tools;

import static ru.m210projects.Build.Engine.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ru.m210projects.Build.filehandle.fs.FileEntry;

public class SaveManager {

	private final List<SaveInfo> SavList = new ArrayList<>();
	private final HashMap<String, SaveInfo> SavHash = new HashMap<>();
	public static final int Screenshot = MAXTILES - 1;
	
	public static class SaveInfo implements Comparable<SaveInfo> {
		public String name;
		public long time;
		public FileEntry entry;

		public SaveInfo(String name, long time, FileEntry entry) {
			update(name, time, entry);
		}
		
		public void update(String name, long time, FileEntry entry)
		{
			this.name = name;
			this.time = time;
			this.entry = entry;
		}

		@Override
		public int compareTo(SaveInfo obj) {
			return (obj.time < this.time)? -1 : 1;
		}
	}

	public SaveInfo getSlot(int num)
	{
		return SavList.get(num);
	}
	
	public List<SaveInfo> getList()
	{
		return SavList;
	}
	
	public void add(String savname, long time, FileEntry entry)
	{
		String filename = entry.getName();
		SaveInfo info;
		if((info = SavHash.get(filename)) == null) {
			info = new SaveInfo(savname, time, entry);
			SavList.add(0, info);
			SavHash.put(filename, info);
		} else {
			SavList.remove(info);
			info.update(savname, time, entry);
			SavList.add(0, info);
		}
	}
	
	public void delete(FileEntry entry) {
		SaveInfo info;
		if((info = SavHash.get(entry.getName())) != null) {
			if (entry.exists() && entry.delete()) {
				SavList.remove(info);
				entry.getParent().revalidate();
			}
		} 
	}
	
	public FileEntry getLast()
	{
		if(SavList.size() > 0) {
			return SavList.get(0).entry;
		}
		return null;
	}
	
	public void sort()
	{
		Collections.sort(SavList);
	}
}
