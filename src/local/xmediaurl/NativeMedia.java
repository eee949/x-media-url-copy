// SPDX-License-Identifier: GPL-3.0-only
package local.xmediaurl;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.*;
/** Native fields pinned to X 12.19.1; checked by the patch before injection. */
public final class NativeMedia {
 private NativeMedia(){}
 public static final class Variant {
  public final String url,label;final int bitrate;
  Variant(String url,String label,int bitrate){this.url=url;this.label=label;this.bitrate=bitrate;}
 }
 private static Object field(Object object,String name)throws ReflectiveOperationException {
  for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(name);
 }
 public static List<Variant> variants(Object entity)throws ReflectiveOperationException {
  if(entity==null)throw new IllegalArgumentException("メディアがありません");
  List<Variant> videos=new ArrayList<>();Set<String> seen=new HashSet<>();Object info=field(entity,"r");
  if(info!=null){
   Object items=field(info,"c");if(!(items instanceof List))throw new IllegalArgumentException("動画URLを取得できません");
   for(Object item:(List<?>)items){String mime=(String)field(item,"c");
    if(!"video/mp4".equals(mime)&&!"video/webm".equals(mime))continue;
    String url=MediaUrls.requireTwimg((String)field(item,"b"));if(!seen.add(url))continue;
    Object rate=field(item,"a");int bitrate=rate instanceof Number?((Number)rate).intValue():0;
    Matcher resolution=Pattern.compile("/(\\d+x\\d+)/").matcher(url);
    String label=resolution.find()?resolution.group(1):bitrate>0?(bitrate/1000)+" kbps":"動画URL";
    videos.add(new Variant(url,label,bitrate));
   }
   if(videos.isEmpty())throw new IllegalArgumentException("コピーできるMP4/WebMのURLがありません");
   videos.sort((a,b)->Integer.compare(b.bitrate,a.bitrate));return videos;
  }
  return Collections.singletonList(new Variant(MediaUrls.requireTwimg((String)field(entity,"n")),"画像URL",0));
 }
}
