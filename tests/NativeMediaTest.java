// SPDX-License-Identifier: GPL-3.0-only
import local.xmediaurl.*;import java.util.*;
public class NativeMediaTest {
 public static class Entity {public String n;public Info r;Entity(String image,Info video){n=image;r=video;}}
 public static class Info {public List<Video> c;Info(Video... variants){c=Arrays.asList(variants);}}
 public static class Video {public int a;public String b,c;Video(int rate,String url,String mime){a=rate;b=url;c=mime;}}
 public static class Action {public int a,b;public String c;public Action(int icon,int id,String label){a=icon;b=id;c=label;}}
 static void yes(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  String image="https://pbs.twimg.com/media/photo.jpg?name=orig&format=jpg";
  yes(NativeMedia.variants(new Entity(image,null)).get(0).url.equals("https://pbs.twimg.com/media/photo.jpg"));
  var video=new Entity(image,new Info(new Video(0,"https://video.twimg.com/a.m3u8","application/x-mpegURL"),new Video(100,"https://video.twimg.com/v/320x180/a.mp4?tag=1","video/mp4"),new Video(500,"https://video.twimg.com/v/1280x720/a.mp4?tag=2","video/mp4")));
  var variants=NativeMedia.variants(video);yes(variants.size()==2&&variants.get(0).label.equals("1280x720")&&variants.get(0).url.endsWith("a.mp4"));
  yes(NativeMedia.variants(new Entity(image,new Info(new Video(0,"https://video.twimg.com/tweet_video/HV1awM4bIAA37hy.mp4?tag=1","video/mp4")))).get(0).url.equals("https://video.twimg.com/tweet_video/HV1awM4bIAA37hy.mp4"));
  try{NativeMedia.variants(new Entity(image,new Info(new Video(0,"https://video.twimg.com/a.m3u8","application/x-mpegURL"))));throw new AssertionError();}catch(IllegalArgumentException expected){}
  try{MediaUrls.requireTwimg("https://twimg.com.evil.test/a.jpg?x=1");throw new AssertionError();}catch(IllegalArgumentException expected){}
  for(int id:new int[]{102,202,203}){var items=new ArrayList<Action>();Action original=new Action(1,id,"native save");items.add(original);MenuHooks.appendSheet(items);MenuHooks.appendSheet(items);yes(items.size()==2&&items.get(0)==original&&items.get(1).b==MenuHooks.COPY);}
  System.out.println("PASS: native image/video/GIF URLs, query stripping, video resolution sorting, HLS exclusion, host checking, copy-only native menus without duplicates");
 }
}
