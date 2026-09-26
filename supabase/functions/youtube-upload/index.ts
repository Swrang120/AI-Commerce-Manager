import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const supabaseUrl=Deno.env.get("SUPABASE_URL")!;
const serviceRoleKey=Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const admin=createClient(supabaseUrl,serviceRoleKey,{auth:{persistSession:false,autoRefreshToken:false}});

function json(data:unknown,status=200){return new Response(JSON.stringify(data),{status,headers:{"Content-Type":"application/json"}});}

async function refreshAccessToken(){
  const {data:connection,error:connectionError}=await admin.from("youtube_connections").select("id").eq("status","connected").limit(1).maybeSingle();
  if(connectionError||!connection)throw new Error("YouTube is not connected.");
  const {data:refreshToken,error:tokenError}=await admin.rpc("youtube_get_refresh_token");
  if(tokenError||!refreshToken)throw new Error("YouTube refresh token is unavailable.");
  const clientId=Deno.env.get("YOUTUBE_CLIENT_ID"),clientSecret=Deno.env.get("YOUTUBE_CLIENT_SECRET");
  if(!clientId||!clientSecret)throw new Error("YouTube OAuth secrets are not configured.");
  const r=await fetch("https://oauth2.googleapis.com/token",{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:new URLSearchParams({client_id:clientId,client_secret:clientSecret,refresh_token:refreshToken,grant_type:"refresh_token"})});
  const t=await r.text();
  if(!r.ok)throw new Error("Google token refresh failed: "+t.slice(0,300));
  return JSON.parse(t).access_token as string;
}

async function uploadJob(jobId:string){
  const {data:job,error}=await admin.from("video_jobs").select("*").eq("id",jobId).single();
  if(error||!job)throw new Error("Video job not found.");
  if(!job.video_url)throw new Error("Video has no external R2 URL yet.");
  if(job.youtube_video_id)return {video_id:job.youtube_video_id,youtube_url:job.youtube_url};

  const accessToken=await refreshAccessToken();
  const vr=await fetch(job.video_url);
  if(!vr.ok)throw new Error("Could not fetch the R2 video.");
  const bytes=new Uint8Array(await vr.arrayBuffer());
  const localizedTitle = job.localized_title || job.title || "E-commerce Shoping Product Video";
  const localizedDescription = job.localized_description || job.description || "AI product video created by E-commerce Shoping.";
  const localeTag = [job.language_code, job.country_code].filter(Boolean).join("-");
  const metadata={snippet:{title:String(localizedTitle).slice(0,100),description:String(localizedDescription).slice(0,5000),categoryId:"22",tags:["E-commerce Shoping","product video","shopping",...(localeTag?[localeTag]:[])]},status:{privacyStatus:"private",selfDeclaredMadeForKids:false,containsSyntheticMedia:true}};

  const start=await fetch("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status",{method:"POST",headers:{Authorization:"Bearer "+accessToken,"Content-Type":"application/json; charset=UTF-8","X-Upload-Content-Type":"video/mp4","X-Upload-Content-Length":String(bytes.byteLength)},body:JSON.stringify(metadata)});
  const st=await start.text();
  if(!start.ok)throw new Error("YouTube upload session failed: "+st.slice(0,500));
  const location=start.headers.get("Location");
  if(!location)throw new Error("YouTube did not return an upload URL.");

  const put=await fetch(location,{method:"PUT",headers:{Authorization:"Bearer "+accessToken,"Content-Type":"video/mp4","Content-Length":String(bytes.byteLength)},body:bytes});
  const pt=await put.text();
  if(!put.ok)throw new Error("YouTube video upload failed: "+pt.slice(0,500));
  const result=JSON.parse(pt);
  if(!result.id)throw new Error("YouTube upload completed without a video ID.");
  const youtubeUrl="https://www.youtube.com/watch?v="+result.id;
  await admin.from("video_jobs").update({youtube_video_id:result.id,youtube_url:youtubeUrl,youtube_upload_status:"uploaded",youtube_uploaded_at:new Date().toISOString(),youtube_error:null,updated_at:new Date().toISOString()}).eq("id",jobId);
  return {video_id:result.id,youtube_url:youtubeUrl};
}

Deno.serve(async(req:Request)=>{
  if(req.method!=="POST")return json({error:"POST required"},405);
  try{
    const auth=req.headers.get("Authorization");
    if(!auth||auth.replace(/^Bearer\s+/i,"")!==serviceRoleKey)return json({error:"Service authorization required"},401);
    const body=await req.json(); if(!body.job_id)return json({error:"job_id is required"},400);
    await admin.from("video_jobs").update({youtube_upload_status:"uploading",youtube_error:null,updated_at:new Date().toISOString()}).eq("id",body.job_id);
    return json({ok:true,...await uploadJob(body.job_id)});
  }catch(e){
    const message=e instanceof Error?e.message:"YouTube upload failed";
    try{const b=await req.clone().json();if(b.job_id)await admin.from("video_jobs").update({youtube_upload_status:"failed",youtube_error:message.slice(0,1000),updated_at:new Date().toISOString()}).eq("id",b.job_id);}catch{}
    return json({error:message},500);
  }
});
