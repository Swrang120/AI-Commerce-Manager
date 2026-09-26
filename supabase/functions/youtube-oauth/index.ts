import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const clientId = Deno.env.get("YOUTUBE_CLIENT_ID");
const clientSecret = Deno.env.get("YOUTUBE_CLIENT_SECRET");
const redirectUri = Deno.env.get("YOUTUBE_REDIRECT_URI") || `${supabaseUrl}/functions/v1/youtube-oauth`;
const siteUrl = Deno.env.get("SITE_URL") || "https://swrang120.github.io/Ecommerce-Shopping-/";
const admin = createClient(supabaseUrl, serviceRoleKey, { auth: { persistSession: false, autoRefreshToken: false } });
const SCOPE = "https://www.googleapis.com/auth/youtube.upload";

function page(title:string,message:string){
  return new Response(`<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${title}</title><style>body{font-family:system-ui;background:#07111f;color:#fff;padding:32px}main{max-width:680px;margin:auto;padding:24px;border:1px solid #263247;border-radius:18px;background:#0d1728}a{color:#6ee7b7}</style></head><body><main><h2>${title}</h2><p>${message}</p><p><a href="${siteUrl}">Return to E-commerce Shoping</a></p></main></body></html>`,{headers:{"Content-Type":"text/html; charset=utf-8"}});
}
function state(){const b=crypto.getRandomValues(new Uint8Array(32));return Array.from(b).map(x=>x.toString(16).padStart(2,"0")).join("");}

Deno.serve(async(req:Request)=>{
  try{
    if(!clientId||!clientSecret)return page("YouTube is not configured","Add YOUTUBE_CLIENT_ID and YOUTUBE_CLIENT_SECRET to the production Edge Function secrets.");
    const u=new URL(req.url),code=u.searchParams.get("code"),oauthError=u.searchParams.get("error");
    if(oauthError)return page("YouTube connection cancelled","Google did not authorize the connection.");
    if(!code){
      const s=state();
      await admin.from("youtube_oauth_states").insert({state:s,expires_at:new Date(Date.now()+10*60*1000).toISOString()});
      const a=new URL("https://accounts.google.com/o/oauth2/v2/auth");
      a.searchParams.set("client_id",clientId);a.searchParams.set("redirect_uri",redirectUri);a.searchParams.set("response_type","code");
      a.searchParams.set("scope",SCOPE);a.searchParams.set("access_type","offline");a.searchParams.set("prompt","consent");a.searchParams.set("state",s);
      return Response.redirect(a.toString(),302);
    }
    const s=u.searchParams.get("state");
    if(!s)return page("OAuth error","Missing OAuth state.");
    const {data:row}=await admin.from("youtube_oauth_states").select("state,expires_at").eq("state",s).maybeSingle();
    if(!row||new Date(row.expires_at).getTime()<Date.now())return page("OAuth expired","Start the YouTube connection again.");
    await admin.from("youtube_oauth_states").delete().eq("state",s);

    const token=await fetch("https://oauth2.googleapis.com/token",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:new URLSearchParams({code,client_id:clientId,client_secret:clientSecret,redirect_uri:redirectUri,grant_type:"authorization_code"})});
    const tokenText=await token.text();
    if(!token.ok)throw new Error("Google token exchange failed: "+tokenText.slice(0,300));
    const tokens=JSON.parse(tokenText);
    if(!tokens.refresh_token)throw new Error("Google did not return a refresh token. Start the connection again and approve offline access.");

    const ch=await fetch("https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true",{headers:{Authorization:"Bearer "+tokens.access_token}});
    const chText=await ch.text();
    if(!ch.ok)throw new Error("Could not read the YouTube channel: "+chText.slice(0,300));
    const channel=JSON.parse(chText).items?.[0];
    if(!channel)throw new Error("No YouTube channel was returned for this Google account.");

    const stored=await admin.rpc("youtube_store_refresh_token",{p_token:tokens.refresh_token});
    if(stored.error)throw stored.error;
    await admin.from("youtube_connections").delete().neq("id","00000000-0000-0000-0000-000000000000");
    const ins=await admin.from("youtube_connections").insert({channel_id:channel.id,channel_title:channel.snippet?.title||"YouTube channel",connected_at:new Date().toISOString(),updated_at:new Date().toISOString(),status:"connected"});
    if(ins.error)throw ins.error;
    return page("YouTube connected","Channel \"" +(channel.snippet?.title||"YouTube")+"\" is connected. Automatic uploads can now use this authorization.");
  }catch(e){return page("YouTube connection error",e instanceof Error?e.message:"Unexpected error");}
});
