package edu.wit.yeatesg.mps.phase0.otherdatatypes;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class ReflectionTools
{
	public static final String DONT_UPDATE_TAG = "$";

	public static String fieldsToString(String regex, Object instance, Class<?> c)
	{	
		return fieldsToString(regex, instance, c, null);
	}
	
	public static String fieldsToString(String regex, Object instance, Class<?> c, String[] excluding)
	{	
		Field[] fields = c.getDeclaredFields();
		String s = "";
		int numUpdatableFields = ReflectionTools.getNumUpdatableFields(c);
		int index = 0;
		for (Field f : fields)
		{
			f.setAccessible(true);
			try
			{					
				if (!Modifier.isStatic(f.getModifiers()) && ReflectionTools.isUpdatableField(f))
				{
					Object v = arrContains(excluding, f.getName()) ? null : f.get(instance);
					s += v + (index == numUpdatableFields - 1 ? "" : regex); // Don't append regex to end of the last field string
					index++;
				}
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				e.printStackTrace();
			}
		}
		return s;
	}
	
	public static boolean isUpdatableField(Field f)
	{
		return !f.getName().substring(0, 1).equals(DONT_UPDATE_TAG) && !Modifier.isStatic(f.getModifiers());
	}
	
	public static int getNumUpdatableFields(Class<?> c)
	{
		return getFieldsThatUpdate(c).size();
	}
	
	public static ArrayList<Field> getFieldsThatUpdate(Class<?> c)
	{
		ArrayList<Field> list = new ArrayList<>();
		Field[] fields = c.getDeclaredFields();
		for (Field f : fields)
			if (isUpdatableField(f))
				list.add(f);
		return list;
	}
	
	public static <T> boolean arrContains(T[] arr, T entry)
	{
		if (arr != null)
		{
			for (T aT : arr)
				if (aT.equals(entry))
					return true;
			return false;
		}
		return false;
	}
	
	public static ArrayList<Field> getFieldsThatDontUpdate(Class<?> c)
	{
		ArrayList<Field> list = new ArrayList<>();
		Field[] fields = c.getDeclaredFields();
		for (Field f : fields)
			if (!isUpdatableField(f))
				list.add(f);
		return list;
	}
}
