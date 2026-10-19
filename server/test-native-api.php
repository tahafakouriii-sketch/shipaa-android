<?php
// Native renderer fixture: private raw metadata must never be read by bridge.
define('ABSPATH',__DIR__);$routes=[];$user=0;$enrolled=false;$html='';
function add_action($name,$fn,$priority=10){if($name==='rest_api_init')$fn();}
function add_filter($name,$fn,$priority=10,$args=1){}
function register_rest_route($ns,$path,$args){global $routes;$routes[$path]=$args;}
function is_user_logged_in(){global $user;return $user>0;}
function wp_get_current_user(){global $user;return (object)['ID'=>$user,'user_email'=>'fixture@example.invalid','display_name'=>'Fixture'];}
function studi_has_bought_items($uid,$pid){global $enrolled;return $enrolled;}
function absint($v){return abs((int)$v);}
function get_post_status($id){return 'publish';}
function get_post_type($id){return 'product';}
function post_password_required($id){return false;}
function shipaa_cf_native($id){global $html;return $html;}
function wp_parse_url($url,$component){return parse_url($url,$component);}
function esc_url_raw($s){return $s;}
class WP_REST_Response{public $data;function __construct($data){$this->data=$data;}function header($k,$v){}}
require __DIR__.'/native-api.php';
function check($condition,$message){if(!$condition)throw new Exception($message);}
$route=$routes['/courses/(?P<id>\d+)/lessons'];$request=['id'=>174];
check(!$route['permission_callback']($request),'Guest must be denied');$user=5;
check(!$route['permission_callback']($request),'Non-enrolled user must be denied');$enrolled=true;
check($route['permission_callback']($request),'Enrolled user should pass');
$html='<div class="course-lesson"><h4>Locked</h4><div class="lessonaccessdenied"></div><video src="https://dl.shipaa.ir/private.mp4"></video></div><div class="course-lesson"><h4>Open</h4><video><source src="https://dl.shipaa.ir/open.mp4"></video><a href="https://dl.shipaa.ir/open.mp4">Duplicate</a><a href="https://evil.example/x.mp4">Bad</a><a href="http://dl.shipaa.ir/insecure.mp4">Bad</a></div>';
$result=$route['callback']($request)->data['lessons'];
check(count($result)===2,'Expected locked row and one deduplicated video');
check($result[0]['url']==='' && !$result[0]['available'],'Locked video leaked');
check($result[1]['url']==='https://dl.shipaa.ir/open.mp4','Wrong URL exported');
echo "Permission and media filtering tests passed\n";
// Account routes must bind writes and queries to the authenticated user.
$user=0;
foreach(['/courses','/orders'] as $path)check(!$routes[$path]['permission_callback'](),'Private route allowed guest');
foreach($routes['/profile'] as $route)check(!$route['permission_callback'](),'Profile allowed guest');
$user=5;$writes=[];$orderQuery=[];
function get_current_user_id(){global $user;return $user;}
function sanitize_text_field($s){return trim(strip_tags($s));}
function get_avatar_url($id){return '';}
function get_user_meta($id,$key,$single){return '';}
function wp_update_user($data){global $writes;$writes[]=$data;return $data['ID'];}
function update_user_meta($id,$key,$value){global $writes;$writes[]=[$id,$key,$value];}
function is_wp_error($v){return $v instanceof WP_Error;}
function wc_get_orders($q){global $orderQuery;$orderQuery=$q;return [];}
class WP_Error{function __construct($a,$b,$c){}}
class ProfileRequest {public $data;function __construct($data){$this->data=$data;}function get_param($k){return $this->data[$k]??null;}}
$write=$routes['/profile'][1]['callback'];
check($write(new ProfileRequest(['first_name'=>'','last_name'=>'Test'])) instanceof WP_Error,'Invalid profile accepted');
check(count($writes)===0,'Invalid profile changed data');
$write(new ProfileRequest(['id'=>999,'role'=>'administrator','first_name'=>'Test','last_name'=>'Learner','phone'=>'۰۹۱۲۱۲۳۴۵۶۷','province'=>'','city'=>'']));
check($writes[0]['ID']===5 && !isset($writes[0]['role']),'Profile accepted arbitrary target or role');
foreach(array_slice($writes,1) as $w)check($w[0]===5,'Metadata targeted another user');
check($writes[3][2]==='09121234567','Persian phone normalization failed');
$routes['/orders']['callback']();check($orderQuery['customer_id']===5,'Orders not scoped to current user');
echo "Account isolation and validation tests passed\n";
