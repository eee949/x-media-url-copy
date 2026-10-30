// SPDX-License-Identifier: GPL-3.0-only
package local.xmediaurl;
import java.net.URI;
import java.util.Locale;
/** Validate the selected media URL and omit its query when copying. */
public final class MediaUrls {
 private MediaUrls(){}
 public static String requireTwimg(String url){
  if(url==null)throw new IllegalArgumentException("メディアURLがありません");
  URI uri=URI.create(url);String host=uri.getHost();
  if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||uri.getRawUserInfo()!=null)throw new IllegalArgumentException("メディアURLを確認できません");
  host=host.toLowerCase(Locale.ROOT);
  if(!host.equals("twimg.com")&&!host.endsWith(".twimg.com"))throw new IllegalArgumentException("twimg.comのメディアURLではありません");
  int query=url.indexOf('?');
  return query<0?url:url.substring(0,query);
 }
}
