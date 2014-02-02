package helios.util.nio.test

import org.scalatest._

import java.nio.file.{Paths, Files}

import helios.util.nio.FileOps._

class FileOps extends FlatSpec with Matchers {
  "FileOps" should "be able to create a file without attributes" in {
    val file = "./testfile1"

    createFile(file).recover {
      case f: Throwable => {
        println(f)
        "Nope"
      }
    }.get.toString should be(file)
    Files.deleteIfExists(Paths.get(file)) should be(true)
  }

  it should "fail if trying to create a file without sufficient permissions" in {
    val file = "/rootfile"

    createFile(file).recover {
      case f: java.nio.file.AccessDeniedException => {
        println(f)
        "Nope"
      }
      case _ => "Something wrong happened"
    }.get.toString should be("Nope")
  }

  it should "be able to delete a file if it exists" in {
    val file = "./testfile1"
    createFile(file)
    val res = deleteIfExists(file)
    res.isSuccess should be(true)
    res.get should be (true)
  }

  it should "fail to delete a file if it does not exist" in {
    val file = "./somerandomfilethatshouldnotexist"
    val res = deleteIfExists(file)
    res.isSuccess should be(true)
    res.get should be(false)
  }

//  it should "fail to delete a file without sufficient permissions" in {
//    //Think of something clever..
//  }
}