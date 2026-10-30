// SPDX-License-Identifier: GPL-3.0-only
package local.xmediaurl;
import android.app.*;import android.content.*;import android.view.*;import android.widget.*;
import java.lang.reflect.*;import java.util.*;
/** X 12.19.1 menu adapters. Each listener captures its own media, not a global selection. */
public final class MenuHooks {
 public static final int COPY=0x786e02;
 private static Object field(Object o,String name)throws Exception{
  for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException next){}
  throw new NoSuchFieldException(name);
 }
 private static Object call(Object o,String name,Class<?>[] types,Object... values)throws Exception{return o.getClass().getMethod(name,types).invoke(o,values);}
 private static Context context(Object gallery)throws Exception{return (Context)field(gallery,"b");}
 private static void error(Context c,Exception e){android.util.Log.e("XMediaUrlCopy","Media selection failed",e);if(c!=null)new android.os.Handler(android.os.Looper.getMainLooper()).post(()->Toast.makeText(c,"メディアURLを取得できませんでした",1).show());}
 @SuppressWarnings({"rawtypes","unchecked"})
 public static void appendSheet(List items){try{
  Object nativeSave=null;
  for(Object i:items){int id=(Integer)field(i,"b");if(id==COPY)return;if(id==102||id==202||id==203)nativeSave=i;}
  if(nativeSave==null)return;
  Object added=nativeSave.getClass().getConstructor(int.class,int.class,String.class).newInstance((Integer)field(nativeSave,"a"),COPY,"メディアURLをコピー");
  items.add(items.indexOf(nativeSave)+1,added);
 }catch(Exception e){android.util.Log.e("XMediaUrlCopy","Cannot add sheet action",e);}}
 public static boolean handleSheet(Object listener,int index){try{
  List<?> items=(List<?>)field(listener,"a");if(index<0||index>=items.size()||(Integer)field(items.get(index),"b")!=COPY)return false;
  Context c=(Context)field(field(listener,"b"),"d");copySelected(c,field(listener,"d"));return true;
 }catch(Exception e){android.util.Log.e("XMediaUrlCopy","Sheet action failed",e);return false;}}
 public static boolean handleSheet(Object listener,Dialog dialog,int unused,int index){boolean handled=handleSheet(listener,index);if(handled&&dialog!=null)dialog.dismiss();return handled;}
 public static void addGalleryMenu(Object gallery,Object unused,Menu menu){addGalleryMenu(gallery,menu);}
 public static void addGalleryMenu(Object gallery,Menu menu){
  if(menu.findItem(COPY)==null)menu.add(0,COPY,11,"メディアURLをコピー").setOnMenuItemClickListener(i->{try{
   Object adapter=field(gallery,"S");int index=(Integer)field(gallery,"T");
   Object selected=call(adapter,"q",new Class<?>[]{int.class},index);
   copySelected(context(gallery),field(selected,"b"));
  }catch(Exception e){try{error(context(gallery),e);}catch(Exception ignored){android.util.Log.e("XMediaUrlCopy","Gallery unavailable",e);}}return true;});
 }
 public static void showMediaActions(Context c,Object entity){
  new AlertDialog.Builder(c).setItems(new String[]{"メディアURLをコピー"},(d,n)->copySelected(c,entity)).setNegativeButton("キャンセル",null).show();
 }
 public static void copySelected(Context c,Object entity){try{
  List<NativeMedia.Variant> variants=NativeMedia.variants(entity);
  if(variants.size()==1){copyUrl(c,variants.get(0).url);return;}
  String[] labels=new String[variants.size()];for(int n=0;n<labels.length;n++)labels[n]=variants.get(n).label;
  new AlertDialog.Builder(c).setTitle("コピーする動画URLの解像度").setItems(labels,(d,n)->{try{copyUrl(c,variants.get(n).url);}catch(Exception e){error(c,e);}}).setNegativeButton("キャンセル",null).show();
 }catch(Exception e){error(c,e);}}
 private static void copyUrl(Context c,String value){
  String url=MediaUrls.requireTwimg(value);
  ClipboardManager clipboard=(ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);
  if(clipboard==null)throw new IllegalStateException("Clipboard unavailable");
  clipboard.setPrimaryClip(ClipData.newPlainText("メディアURL",url));
  if(android.os.Build.VERSION.SDK_INT<33)Toast.makeText(c,"メディアURLをコピーしました",Toast.LENGTH_SHORT).show();
 }
 private static final WeakHashMap<Window,java.lang.ref.WeakReference<ViewerTouch>> viewers=new WeakHashMap<>();
 public static void bindMedia(Object holder,Object item){try{
  Object media=field(item,"b");
  if(media==null)return;
  View root=(View)field(holder,"itemView");
  Activity activity=activity(root.getContext());if(activity==null)throw new IllegalStateException("Viewer activity unavailable");
  Window window=activity.getWindow();
  java.lang.ref.WeakReference<ViewerTouch> reference=viewers.get(window);
  ViewerTouch observer=reference==null?null:reference.get();
  if(observer==null||window.getCallback()!=observer.proxy){
   observer=new ViewerTouch(activity,window.getCallback());window.setCallback(observer.proxy);
   viewers.put(window,new java.lang.ref.WeakReference<>(observer));
  }
  observer.bind(root,media);
 }catch(Exception e){android.util.Log.e("XMediaUrlCopy","Cannot bind media long press",e);}}
 private static Activity activity(Context c){
  while(c instanceof ContextWrapper){if(c instanceof Activity)return (Activity)c;Context next=((ContextWrapper)c).getBaseContext();if(next==c)break;c=next;}
  return c instanceof Activity?(Activity)c:null;
 }
 private static boolean isMedia(View v){
  if(v instanceof SurfaceView||v instanceof TextureView)return true;
  for(Class<?> c=v.getClass();c!=null;c=c.getSuperclass())
   if(c.getName().contains("FrescoDraweeView"))return true;
  return false;
 }
 private static boolean hitsMedia(View v,float x,float y){
  if(!v.isShown())return false;
  android.graphics.Rect bounds=new android.graphics.Rect();
  if(!v.getGlobalVisibleRect(bounds)||!bounds.contains((int)x,(int)y))return false;
  if(isMedia(v))return true;
  if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int n=group.getChildCount()-1;n>=0;n--)if(hitsMedia(group.getChildAt(n),x,y))return true;}
  return false;
 }
 private static final class Binding {
  final java.lang.ref.WeakReference<View> root;final Object media;
  Binding(View root,Object media){this.root=new java.lang.ref.WeakReference<>(root);this.media=media;}
 }
 // Observe Window dispatch before X's children consume touch events. Delegate every
 // event unchanged, preserving swipe, zoom and player controls.
 private static final class ViewerTouch implements InvocationHandler {
  final Window.Callback delegate,proxy;final GestureDetector detector;final ArrayList<Binding> bindings=new ArrayList<>();
  Binding pressed;long last;
  ViewerTouch(Activity activity,Window.Callback delegate){
   this.delegate=delegate;
   detector=new GestureDetector(activity,new GestureDetector.SimpleOnGestureListener(){
    public boolean onDown(MotionEvent e){pressed=null;for(int n=bindings.size()-1;n>=0;n--){Binding binding=bindings.get(n);View root=binding.root.get();if(root==null){bindings.remove(n);continue;}if(hitsMedia(root,e.getRawX(),e.getRawY())){pressed=binding;break;}}return pressed!=null;}
    public void onLongPress(MotionEvent e){Binding selected=pressed;View root=selected==null?null:selected.root.get();long now=android.os.SystemClock.elapsedRealtime();
     if(root!=null&&root.isShown()&&now-last>=1000){last=now;root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);showMediaActions(root.getContext(),selected.media);}}
   });
   proxy=(Window.Callback)Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),new Class<?>[]{Window.Callback.class},this);
  }
  void bind(View root,Object media){bindings.removeIf(b->b.root.get()==null||b.root.get()==root);bindings.add(new Binding(root,media));}
  public Object invoke(Object proxy,Method method,Object[] arguments)throws Throwable {
   if(method.getName().equals("dispatchTouchEvent")&&arguments!=null&&arguments.length==1&&arguments[0] instanceof MotionEvent){
    MotionEvent event=(MotionEvent)arguments[0];try{detector.onTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)pressed=null;}catch(Exception e){android.util.Log.e("XMediaUrlCopy","Viewer gesture failed",e);}
   }
   try{return method.invoke(delegate,arguments);}catch(InvocationTargetException e){throw e.getCause();}
  }
 }
}
