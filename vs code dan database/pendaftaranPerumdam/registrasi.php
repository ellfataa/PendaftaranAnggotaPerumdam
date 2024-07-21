<?php
    // Menyambungkan ke koneksi.php database
    include 'koneksi.php';

    // Inisiasi response array
    $response = array();

    // Untuk menangkap data yang dikirim dari aplikasi
    $nama = $_POST['nama'];
    $nik = $_POST['nik'];
    $id_pekerjaan = $_POST['id_pekerjaan'];
    $alamat = $_POST['alamat'];
    $rt = $_POST['rt'];
    $rw = $_POST['rw'];
    $id_kelurahan = $_POST['id_kelurahan'];
    $id_kecamatan = $_POST['id_kecamatan'];
    $kode_pos = $_POST['kode_pos'];
    $jumlah_penghuni = $_POST['jumlah_penghuni'];
    $latitude = $_POST['latitude'];
    $longitude = $_POST['longitude'];
    $telp_hp = $_POST['telp_hp'];
    $foto_ktp = $_POST['foto_ktp'];
    $foto_rumah = $_POST['foto_rumah'];

    // Untuk menyimpan gambar ke direktori
    function saveBase64Image($base64String, $outputDir) {
        $image_parts = explode(";base64,", $base64String);
        $image_type_aux = explode("image/", $image_parts[0]);
        $image_type = $image_type_aux[1];
        $imageData = base64_decode($image_parts[1]);
        
        $filename = uniqid() . '.' . $image_type;
        $file = $outputDir . $filename;
        file_put_contents($file, $imageData);
        return $filename;
    }

    // Untuk menyimpan gambar ke direktori uploads/Foto KTP/ dan uploads/Foto Rumah/
    $uploadDirKTP = 'uploads/Foto KTP/';
    $uploadDirRumah = 'uploads/Foto Rumah/';

    // Untuk membuat direktori jika belum ada
    if (!file_exists($uploadDirKTP)) {
        mkdir($uploadDirKTP, 0777, true);
    }
    if (!file_exists($uploadDirRumah)) {
        mkdir($uploadDirRumah, 0777, true);
    }

    // Menyimpan gambar ke direktori
    $foto_ktp_filename = saveBase64Image($foto_ktp, $uploadDirKTP);
    $foto_rumah_filename = saveBase64Image($foto_rumah, $uploadDirRumah);

    // Untuk menyimpan data ke database
    $query = "INSERT INTO user (nama, nik, foto_ktp, id_kelurahan, id_kecamatan, id_pekerjaan, alamat, rt, rw, telp_hp, kode_pos, jumlah_penghuni, latitude, longitude, foto_rumah) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    $stmt = $conn->prepare($query);
    $stmt->bind_param("sssssssssssssss", $nama, $nik, $foto_ktp_filename, $id_kelurahan, $id_kecamatan, $id_pekerjaan, $alamat, $rt, $rw, $telp_hp, $kode_pos, $jumlah_penghuni, $latitude, $longitude, $foto_rumah_filename);

    if ($stmt->execute()) {
        $response['status'] = 'OK';
        $response['message'] = 'Data berhasil disimpan';
    } else {
        $response['status'] = 'FAILED';
        $response['message'] = 'Gagal menyimpan data: ' . $stmt->error;
    }

    $stmt->close();

    // Menampilkan response dalam format JSON
    echo json_encode(array("server_response" => array($response)));
    mysqli_close($conn);
?>