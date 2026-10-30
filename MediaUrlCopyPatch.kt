// SPDX-License-Identifier: GPL-3.0-only
package local.xmediaurl.patches

import app.morphe.patcher.patch.*
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val mediaUrlCopyPatch = bytecodePatch(
    name="Copy media URL",
    description="X 12.19.1: copy the selected image/video twimg URL from media long-press menus and the gallery overflow menu. Removes the query string. No Piko or Nagi dependency.",
    true
) {
    compatibleWith(Compatibility(packageName="com.twitter.android",name="X",targets=listOf(AppTarget(version="12.19.1-release.0"))))
    extendWith("extensions/media-url-copy.dex")
    execute {
        // Fail during patching when the version-specific native media layout differs.
        fun requireField(owner:String,name:String,type:String) {
            check(mutableClassDefBy(owner).fields.any { it.name==name && it.type==type }) { "Unsupported X media layout: $owner->$name" }
        }
        requireField("Lcom/twitter/model/core/entity/c0;","n","Ljava/lang/String;")
        requireField("Lcom/twitter/model/core/entity/c0;","r","Lcom/twitter/media/av/model/y;")
        requireField("Lcom/twitter/media/av/model/y;","c","Ljava/util/List;")
        requireField("Lcom/twitter/media/av/model/z;","a","I")
        requireField("Lcom/twitter/media/av/model/z;","b","Ljava/lang/String;")
        requireField("Lcom/twitter/media/av/model/z;","c","Ljava/lang/String;")
        // Add a distinct row to the native post-image/video action sheet.
        val sheet=mutableClassDefBy("Lcom/twitter/tweet/action/legacy/c;").methods.single {
            it.name=="z" && it.parameterTypes.map { p->p.toString() }==listOf("Lcom/twitter/model/core/e;","Lcom/twitter/model/core/entity/c0;")
        }
        val empty=sheet.implementation!!.instructions.indexOfFirst {
            val ref=(it as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass=="Ljava/util/List;" && ref.name=="isEmpty"
        }
        check(empty>=0)
        val listRegister=(sheet.implementation!!.instructions[empty] as FiveRegisterInstruction).registerC
        sheet.insertAtLabel(empty,"invoke-static/range {v$listRegister .. v$listRegister}, Llocal/xmediaurl/MenuHooks;->appendSheet(Ljava/util/List;)V")
        val callback=mutableClassDefBy("Lcom/twitter/tweetview/core/ui/mediaoptionssheet/l;").methods.single { it.name=="y1" }
        check(callback.implementation!!.registerCount>=5)
        val first=callback.implementation!!.instructions.first()
        callback.addInstructionsWithLabels(0,"""
            invoke-static/range {p0 .. p3}, Llocal/xmediaurl/MenuHooks;->handleSheet(Ljava/lang/Object;Landroid/app/Dialog;II)Z
            move-result v0
            if-eqz v0, :native_sheet
            return-void
        """.trimIndent(),ExternalLabel("native_sheet",first))

        // Gallery overflow menu. Resolve the current adapter item when clicked, so swiping is safe.
        val galleryClass=mutableClassDefBy("Lcom/twitter/app/gallery/c0;")
        val gallery=galleryClass.methods.single { it.name=="d0" && it.parameterTypes.last().toString()=="Landroid/view/Menu;" }
        val galleryWrapper=gallery.wrapper("""
            invoke-virtual/range {p0 .. p2}, ${gallery.definingClass}->xmediaurlOriginal_d0(${gallery.parameterTypes.joinToString("")})Z
            move-result v0
            invoke-static/range {p0 .. p2}, Llocal/xmediaurl/MenuHooks;->addGalleryMenu(Ljava/lang/Object;Ljava/lang/Object;Landroid/view/Menu;)V
            return v0
        """.trimIndent())
        galleryClass.methods.remove(gallery)
        gallery.setName("xmediaurlOriginal_d0")
        galleryClass.methods.add(gallery)
        galleryClass.methods.add(galleryWrapper)
        // Separate call frames preserve the bound item even when native code reuses p1.
        for(holder in listOf("z0","h1")) {
            val holderClass=mutableClassDefBy("Lcom/twitter/app/gallery/$holder;")
            val bind=holderClass.methods.single {
                it.name=="p" && it.parameterTypes.first().toString()=="Lcom/twitter/app/gallery/item/a;"
            }
            val bindWrapper=bind.wrapper("""
                invoke-virtual/range {p0 .. p3}, ${bind.definingClass}->xmediaurlOriginal_p(${bind.parameterTypes.joinToString("")})V
                invoke-static/range {p0 .. p1}, Llocal/xmediaurl/MenuHooks;->bindMedia(Ljava/lang/Object;Ljava/lang/Object;)V
                return-void
            """.trimIndent())
            holderClass.methods.remove(bind)
            bind.setName("xmediaurlOriginal_p")
            holderClass.methods.add(bind)
            holderClass.methods.add(bindWrapper)
        }
    }
}

private fun MutableMethod.insertAtLabel(index:Int,code:String) {
    val anchor=implementation!!.instructions[index]
    val labels=anchor.location.labels.toList()
    addInstructions(index,code)
    // Existing branch targets must enter the inserted hook too.
    val insertedLocation=implementation!!.instructions[index].location
    anchor.location.labels.removeAll(labels.toSet())
    insertedLocation.labels.addAll(labels)
}



// These instance methods have only single-register reference parameters. v0 is a
// private result register; all p registers remain untouched across both calls.
private fun MutableMethod.wrapper(code:String):MutableMethod {
    check(parameterTypes.all { it.toString().startsWith("L") })
    return MutableMethod(ImmutableMethod(definingClass,name,parameters,returnType,
        accessFlags,annotations,hiddenApiRestrictions,
        ImmutableMethodImplementation(parameterTypes.size+2,emptyList(),emptyList(),emptyList())
    )).apply { addInstructions(0,code) }
}
