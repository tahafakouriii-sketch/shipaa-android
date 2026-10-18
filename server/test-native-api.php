<?php
// Native renderer fixture: private raw metadata must never be read by bridge.
define('ABSPATH',__DIR__);$routes=[];$user=0;$enrolled=false;$html='';
function add_action($name,$fn,$priority=10){if($name==='rest_api_init')$fn();}
function register_rest_route($ns,$path,$args){global $routes;$routes[$path]=$args;}
function is_user_logged_in(){global $user;return $user>0;}
function wp_get_current_user(){global $user;return (object)['ID'=>$user,'user_email'=>'fixture@example.invalid'];}
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
