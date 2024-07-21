<?php
    error_reporting(E_ALL ^ E_NOTICE ^ E_DEPRECATED ^ E_WARNING);

    $host = 'localhost';
    $user = 'root';
    $pass = '';
    $db = 'pendaftaran_pdam';

    $conn = mysqli_connect($host, $user, $pass, $db) or die('Database gagal terhubung');
    date_default_timezone_set('Asia/Jakarta');
?>