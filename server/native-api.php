<?php
/** Shipaa native learner bridge. Install as a Code Snippet without opening tag. */
if (!defined('ABSPATH')) exit;
function shipaa_native_enrolled($pid) {
    $u=wp_get_current_user();
    if (!$u->ID) return false;
    if (function_exists('studi_has_bought_items') && in_array(studi_has_bought_items($u->ID,$pid),array(true,1,'1','true'),true)) return true;
    return function_exists('wc_customer_bought_product') && wc_customer_bought_product($u->user_email,$u->ID,$pid);
}
function shipaa_native_response($data) {
    $r=new WP_REST_Response($data); $r->header('Cache-Control','private, no-store, max-age=0'); return $r;
}
add_action('template_redirect',function(){
    if (!isset($_GET['shipaa_native_session'])) return;
    nocache_headers(); header('Cache-Control: private, no-store, max-age=0');
    header('X-Content-Type-Options: nosniff');
    $u=wp_get_current_user();
    wp_send_json(array('id'=>$u->ID,'name'=>$u->ID?$u->display_name:'','nonce'=>$u->ID?wp_create_nonce('wp_rest'):''));
},0);
add_action('rest_api_init',function(){
    register_rest_route('shipaa-native/v1','/courses',array('methods'=>'GET','permission_callback'=>function(){return is_user_logged_in();},'callback'=>function(){
        if (!function_exists('wc_get_product')) return new WP_Error('unavailable','آموزش موقتاً در دسترس نیست',array('status'=>503));
        $out=array();
        $ids=get_posts(array('post_type'=>'product','post_status'=>'publish','posts_per_page'=>200,'fields'=>'ids','meta_key'=>'_studiare_course','meta_value'=>'yes'));
        foreach($ids as $id) if(!post_password_required($id) && shipaa_native_enrolled($id)) {
            $out[]=array('id'=>$id,'title'=>get_the_title($id),'image'=>get_the_post_thumbnail_url($id,'medium_large')?:'','duration'=>get_post_meta($id,'_studiare_course_duration',true));
        }
        return shipaa_native_response(array('courses'=>$out));
    }));
    register_rest_route('shipaa-native/v1','/courses/(?P<id>\d+)/lessons',array('methods'=>'GET','permission_callback'=>function($r){
        $id=absint($r['id']);
        return is_user_logged_in() && get_post_status($id)==='publish' && get_post_type($id)==='product' && !post_password_required($id) && shipaa_native_enrolled($id);
    },'callback'=>function($r){
        if (!function_exists('shipaa_cf_native') || !class_exists('DOMDocument')) return new WP_Error('unavailable','نمایش جلسات در دسترس نیست',array('status'=>503));
        // Parse ONLY native, permission-filtered output. Never expose raw lesson metadata.
        $html=shipaa_cf_native(absint($r['id']));
        $dom=new DOMDocument(); $previous=libxml_use_internal_errors(true);
        $dom->loadHTML('<?xml encoding="UTF-8"><html><body>'.$html.'</body></html>',LIBXML_NONET);
        libxml_clear_errors();libxml_use_internal_errors($previous);$xp=new DOMXPath($dom);$out=array();
        $rows=$xp->query('//*[contains(concat(" ",normalize-space(@class)," ")," course-lesson ")]');
        foreach($rows as $index=>$row){
            $titles=$xp->query('.//h4',$row);$title=$titles->length?trim($titles->item(0)->textContent):'جلسه '.($index+1);
            $locked=$xp->query('.//*[contains(@class,"lessonaccessdenied") or contains(@class,"lesson-locked-message")]',$row)->length>0;
            $videos=array();
            if(!$locked) foreach($xp->query('.//video[@src] | .//video/source[@src] | .//a[@href]',$row) as $node){
                $url=html_entity_decode($node->getAttribute($node->tagName==='a'?'href':'src'),ENT_QUOTES,'UTF-8');
                $host=strtolower((string)wp_parse_url($url,PHP_URL_HOST));$path=(string)wp_parse_url($url,PHP_URL_PATH);
                if(wp_parse_url($url,PHP_URL_SCHEME)==='https' && in_array($host,array('shipaa.ir','dl.shipaa.ir'),true) && preg_match('/\.mp4$/i',$path)) $videos[$url]=$url;
            }
            $i=0; foreach($videos as $url){$i++;$out[]=array('id'=>hash('sha256',$r['id'].'|'.$url),'title'=>$title.(count($videos)>1?' · '.$i:''),'url'=>esc_url_raw($url),'available'=>true);}
            if(!$videos)$out[]=array('id'=>'row-'.$index,'title'=>$title,'url'=>'','available'=>false,'reason'=>$locked?'این جلسه هنوز برای حساب شما باز نیست':'این محتوا در نسخه وب قابل مشاهده است');
        }
        return shipaa_native_response(array('lessons'=>$out));
    }));
});
