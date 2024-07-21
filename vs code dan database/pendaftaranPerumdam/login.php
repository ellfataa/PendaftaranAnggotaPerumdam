<?php
    include 'koneksi.php';

    // Login User menggunakan nama dan nik
    if($conn){
        $nama = $_POST['nama'];
        $nik = $_POST['nik'];

        $query = "SELECT * FROM user WHERE nama = '$nama' AND nik = '$nik'";
        $sql = mysqli_query($conn, $query);
        $response = array();

        $row = mysqli_num_rows($sql);

        if($row > 0){
            array_push($response, array('status' => 'OK'));
        }else{
            array_push($response, array('status' => 'FAILED'));
        }
    }else{
        array_push($response, array('status' => 'FAILED'));
    }

    echo json_encode(array("server_response" => $response));
    mysqli_close($conn);

?>