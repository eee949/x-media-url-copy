// SPDX-License-Identifier: GPL-3.0-only
import java.io.*;import java.util.*;
import com.android.tools.smali.dexlib2.*;import com.android.tools.smali.dexlib2.iface.*;import com.android.tools.smali.dexlib2.iface.instruction.*;
public class CheckPatchedApk {
 static Map<String,ClassDef> classes=new HashMap<>();
 static Method find(String owner,String name){for(var m:classes.get(owner).getMethods())if(m.getName().equals(name))return m;throw new AssertionError(owner+name);}
 static void yes(boolean b){if(!b)throw new AssertionError();}
 static int refs(Method method,String target){int n=0;for(var i:method.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r&&r.getReference().toString().contains(target))n++;return n;}
 public static void main(String[] args)throws Exception{
  var container=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());
  for(var entry:container.getDexEntryNames())for(var c:container.getEntry(entry).getDexFile().getClasses())yes(classes.put(c.getType(),c)==null);
  yes(classes.containsKey("Llocal/xmediaurl/NativeMedia;"));
  for(String name:classes.keySet())yes(!name.startsWith("Lapp/morphe/extension/")&&!name.startsWith("Llocal/xmedia/")&&!name.startsWith("Lapp/nagi/"));
  yes(refs(find("Lcom/twitter/tweet/action/legacy/c;","z"),"MenuHooks;->appendSheet")==1);
  yes(refs(find("Lcom/twitter/tweetview/core/ui/mediaoptionssheet/l;","y1"),"MenuHooks;->handleSheet")==1);
  String[] owners={"c0","z0","h1"};
  for(String owner:owners){boolean gallery=owner.equals("c0");String name=gallery?"d0":"p";Method m=find("Lcom/twitter/app/gallery/"+owner+";",name);
   var code=new ArrayList<Instruction>();m.getImplementation().getInstructions().forEach(code::add);
   int params=gallery?3:4;yes(m.getImplementation().getRegisterCount()==params+1);yes(code.size()==(gallery?4:3));
   yes(code.get(0).getOpcode()==Opcode.INVOKE_VIRTUAL_RANGE);var invoke=(RegisterRangeInstruction)code.get(0);yes(invoke.getStartRegister()==1&&invoke.getRegisterCount()==params);
   yes(refs(m,"->xmediaurlOriginal_"+name)==1);yes(refs(m,gallery?"MenuHooks;->addGalleryMenu":"MenuHooks;->bindMedia")==1);
  }
  yes(refs(find("Llocal/xmediaurl/MenuHooks;","copyUrl"),"ClipboardManager;->setPrimaryClip")==1);
  yes(refs(find("Llocal/xmediaurl/MenuHooks$ViewerTouch$1;","onLongPress"),"MenuHooks;->showMediaActions")==1);
  System.out.println("PASS: copy-only standalone patch hooks, preserved argument registers, clipboard and long-press paths; no Piko/Nagi/save/GIF classes; no duplicate classes");
 }
}
