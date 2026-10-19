<?php
/** Shipaa learner API 1.0. Backward compatible with native 0.2 / 0.3. */
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
function shipaa_native_course($id) {
    $p=wc_get_product($id); $teachers=array();
    foreach((array)get_post_meta($id,'shipaa_hub_teacher_ids',true) as $tid){if(get_post_status(absint($tid))==='publish')$teachers[]=get_the_title(absint($tid));}
    $count=0;foreach((array)get_post_meta($id,'lessons_group',true) as $g){$count+=count((array)($g['lessons_list']??array()));}
    return array('id'=>$id,'title'=>get_the_title($id),'image'=>get_the_post_thumbnail_url($id,'medium_large')?:'',
        'duration'=>(string)get_post_meta($id,'_studiare_course_duration',true),'teacher'=>implode('، ',$teachers),'lesson_count'=>$count,
        'description'=>wp_trim_words(wp_strip_all_tags($p?$p->get_short_description():''),65),
        'price'=>$p?html_entity_decode(wp_strip_all_tags($p->get_price_html()),ENT_QUOTES,'UTF-8'):'',
        'url'=>get_permalink($id),'register_url'=>add_query_arg('product_id',$id,home_url('/register/')));
}
function shipaa_native_profile_data() {
    $u=wp_get_current_user();$states=function_exists('WC')?WC()->countries->get_states('IR'):array();
    return array('id'=>$u->ID,'name'=>$u->display_name,'avatar'=>get_avatar_url($u->ID),'first_name'=>get_user_meta($u->ID,'first_name',true),
      'last_name'=>get_user_meta($u->ID,'last_name',true),'phone'=>get_user_meta($u->ID,'billing_phone',true),
      'province'=>get_user_meta($u->ID,'billing_state',true),'city'=>get_user_meta($u->ID,'billing_city',true),'provinces'=>(object)$states);
}
add_action('template_redirect',function(){
    if (!isset($_GET['shipaa_native_session'])) return;
    nocache_headers(); header('Cache-Control: private, no-store, max-age=0');header('X-Content-Type-Options: nosniff');
    $u=wp_get_current_user();wp_send_json(array('id'=>$u->ID,'name'=>$u->ID?$u->display_name:'','nonce'=>$u->ID?wp_create_nonce('wp_rest'):'','api_version'=>'1.0'));
},0);
add_filter('rest_post_dispatch',function($response,$server,$request){
    if(strpos($request->get_route(),'/shipaa-native/v1/')===0 && $response instanceof WP_REST_Response)$response->header('Cache-Control','private, no-store, max-age=0');
    return $response;
},10,3);
add_action('rest_api_init',function(){
    $auth=function(){return is_user_logged_in();};
    register_rest_route('shipaa-native/v1','/catalog',array('methods'=>'GET','permission_callback'=>'__return_true','callback'=>function(){
        if(!function_exists('wc_get_product'))return new WP_Error('unavailable','سرویس آموزش در دسترس نیست',array('status'=>503));
        $ids=get_posts(array('post_type'=>'product','post_status'=>'publish','posts_per_page'=>200,'fields'=>'ids','meta_key'=>'_studiare_course','meta_value'=>'yes'));
        $out=array();foreach($ids as $id){if(!post_password_required($id))$out[]=shipaa_native_course($id);}
        return shipaa_native_response(array('courses'=>$out));
    }));
    register_rest_route('shipaa-native/v1','/courses',array('methods'=>'GET','permission_callback'=>$auth,'callback'=>function(){
        if(!function_exists('wc_get_product'))return new WP_Error('unavailable','سرویس آموزش در دسترس نیست',array('status'=>503));
        $ids=get_posts(array('post_type'=>'product','post_status'=>'publish','posts_per_page'=>200,'fields'=>'ids','meta_key'=>'_studiare_course','meta_value'=>'yes'));
        $out=array();foreach($ids as $id)if(!post_password_required($id)&&shipaa_native_enrolled($id))$out[]=shipaa_native_course($id);
        return shipaa_native_response(array('courses'=>$out));
    }));
    register_rest_route('shipaa-native/v1','/profile',array(
      array('methods'=>'GET','permission_callback'=>$auth,'callback'=>function(){return shipaa_native_response(shipaa_native_profile_data());}),
      array('methods'=>'POST','permission_callback'=>$auth,'callback'=>function($r){
        $id=get_current_user_id();$first=sanitize_text_field((string)$r->get_param('first_name'));$last=sanitize_text_field((string)$r->get_param('last_name'));
        $phone=strtr((string)$r->get_param('phone'),array('۰'=>'0','۱'=>'1','۲'=>'2','۳'=>'3','۴'=>'4','۵'=>'5','۶'=>'6','۷'=>'7','۸'=>'8','۹'=>'9','٠'=>'0','١'=>'1','٢'=>'2','٣'=>'3','٤'=>'4','٥'=>'5','٦'=>'6','٧'=>'7','٨'=>'8','٩'=>'9'));
        $phone=preg_replace('/[\s\-()]+/','',$phone);$province=sanitize_text_field((string)$r->get_param('province'));$city=sanitize_text_field((string)$r->get_param('city'));
        if(!$first||!$last||mb_strlen($first)>70||mb_strlen($last)>70||mb_strlen($city)>100)return new WP_Error('invalid_profile','نام، نام خانوادگی و شهر را بررسی کنید.',array('status'=>400));
        if($phone!==''&&!preg_match('/^(?:09\d{9}|\+989\d{9})$/',$phone))return new WP_Error('invalid_phone','شماره همراه را درست وارد کنید.',array('status'=>400));
        $states=function_exists('WC')?WC()->countries->get_states('IR'):array();
        if($province!==''&&!isset($states[$province]))return new WP_Error('invalid_province','استان را از فهرست انتخاب کنید.',array('status'=>400));
        // Fixed allowlist: no target user ID, role, password or arbitrary meta is accepted.
        $updated=wp_update_user(array('ID'=>$id,'first_name'=>$first,'last_name'=>$last,'display_name'=>trim($first.' '.$last)));
        if(is_wp_error($updated))return $updated;
        foreach(array('billing_first_name'=>$first,'billing_last_name'=>$last,'billing_phone'=>$phone,'billing_state'=>$province,'billing_city'=>$city) as $key=>$value)update_user_meta($id,$key,$value);
        return shipaa_native_response(shipaa_native_profile_data());
      })
    ));
    register_rest_route('shipaa-native/v1','/orders',array('methods'=>'GET','permission_callback'=>$auth,'callback'=>function(){
        if(!function_exists('wc_get_orders'))return new WP_Error('unavailable','سرویس سفارش‌ها در دسترس نیست',array('status'=>503));
        $orders=wc_get_orders(array('customer_id'=>get_current_user_id(),'limit'=>30,'orderby'=>'date','order'=>'DESC'));$out=array();
        foreach($orders as $o){$names=array();foreach($o->get_items() as $i)$names[]=$i->get_name();$out[]=array('id'=>$o->get_id(),'number'=>$o->get_order_number(),'status'=>$o->get_status(),'label'=>wc_get_order_status_name($o->get_status()),'title'=>implode('، ',$names),'amount'=>html_entity_decode(wp_strip_all_tags($o->get_formatted_order_total()),ENT_QUOTES,'UTF-8'),'date'=>$o->get_date_created()?$o->get_date_created()->date_i18n('Y/m/d'):'','url'=>$o->get_view_order_url());}
        return shipaa_native_response(array('orders'=>$out));
    }));
    register_rest_route('shipaa-native/v1','/courses/(?P<id>\d+)/lessons',array('methods'=>'GET','permission_callback'=>function($r){
        $id=absint($r['id']);return is_user_logged_in()&&get_post_status($id)==='publish'&&get_post_type($id)==='product'&&!post_password_required($id)&&shipaa_native_enrolled($id);
    },'callback'=>function($r){
        if(!function_exists('shipaa_cf_native')||!class_exists('DOMDocument'))return new WP_Error('unavailable','نمایش جلسات در دسترس نیست',array('status'=>503));
        $html=shipaa_cf_native(absint($r['id']));$dom=new DOMDocument();$previous=libxml_use_internal_errors(true);
        $dom->loadHTML('<?xml encoding="UTF-8"><html><body>'.$html.'</body></html>',LIBXML_NONET);libxml_clear_errors();libxml_use_internal_errors($previous);$xp=new DOMXPath($dom);$out=array();
        foreach($xp->query('//*[contains(concat(" ",normalize-space(@class)," ")," course-lesson ")]') as $index=>$row){
            $titles=$xp->query('.//h4',$row);$title=$titles->length?trim($titles->item(0)->textContent):'جلسه '.($index+1);$title=preg_replace('/\s+/u',' ',$title);
            $locked=$xp->query('.//*[contains(@class,"lessonaccessdenied") or contains(@class,"lesson-locked-message")]',$row)->length>0;$videos=array();
            // Only inspect permission-filtered rendered media, never raw lesson metadata.
            if(!$locked)foreach($xp->query('.//video[@src] | .//video/source[@src] | .//a[@href]',$row) as $node){
                $url=html_entity_decode($node->getAttribute($node->tagName==='a'?'href':'src'),ENT_QUOTES,'UTF-8');$host=strtolower((string)wp_parse_url($url,PHP_URL_HOST));$path=(string)wp_parse_url($url,PHP_URL_PATH);
                if(wp_parse_url($url,PHP_URL_SCHEME)==='https'&&in_array($host,array('shipaa.ir','dl.shipaa.ir'),true)&&preg_match('/\.mp4$/i',$path))$videos[$url]=$url;
            }
            $i=0;foreach($videos as $url){$i++;$out[]=array('id'=>hash('sha256',$r['id'].'|'.$url),'title'=>$title.(count($videos)>1?' · '.$i:''),'url'=>esc_url_raw($url),'available'=>true);}
            if(!$videos)$out[]=array('id'=>'row-'.$index,'title'=>$title,'url'=>'','available'=>false,'reason'=>$locked?'این جلسه هنوز برای حساب شما باز نیست':'مشاهده محتوای تکمیلی در سایت');
        }
        return shipaa_native_response(array('lessons'=>$out));
    }));
});
