package com.zichun.player;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyAPIC;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MusicData {

    public static boolean isMP3(File file){
        if(file.getName().contains(".mp3")){
            return true;
        }
        return false;

    }

    public static String getMusicPath(File file){
        return file.getAbsolutePath();
    }

    public static String getMusicPathURI(File file){
        return file.toURI().toString();
    }

    public static String getMusicSrcName(File file){
        return file.getName().substring(0, file.getName().length()-4);
    }

    
    public static String getMusicLrcPath(File file){
        int addrLenth = file.getAbsolutePath().length();
        return file.getAbsolutePath().substring(0,addrLenth-4) + ".lrc";
    }

    public static WritableImage getMusicImage(File file){
        //File打开音乐
        File soureceFile = new File(MusicData.getMusicPath(file));
        MP3File mp3File= null;
        try {
            mp3File = new MP3File(soureceFile);
        } catch (IOException | TagException | ReadOnlyFileException | CannotReadException | InvalidAudioFrameException e) {
            throw new RuntimeException(e);
        }

        //获取MP3的tag头文件，获取头文件中的专辑图片信息
        AbstractID3v2Tag tag = mp3File.getID3v2Tag();
        AbstractID3v2Frame frame = (AbstractID3v2Frame)tag.getFrame("APIC");

        FrameBodyAPIC body = (FrameBodyAPIC) frame.getBody();
        //将图片头文件信息存储到字节数组中
        byte[] imageData = body.getImageData();
        //用awt库中自带的字节转图片功能转换成awt类型的图片
        java.awt.Image image = Toolkit.getDefaultToolkit().createImage(imageData,0, imageData.length);
        //将awt图片加载到内存中
        BufferedImage bufferedImage = ImageUtils.toBufferedImage(image);
        //用网上找到的SwingFXUtils转换成javafx的可写image并返回
        //这个时候imageView就可以显示音乐图片了
        return SwingFXUtils.toFXImage(bufferedImage,null);
    }

    public static String getMusicArtistInfo(File file) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        MP3File mp3File = (MP3File) AudioFileIO.read(file);
        //mp3的ID3标签
        Tag tag = mp3File.getID3v2TagAsv24();

        if (tag != null) {
            //歌手
            return tag.getFirst(FieldKey.ARTIST);
        };
        return null;
    }

    public static String getMusicNameInfo(File file) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        MP3File mp3File = (MP3File) AudioFileIO.read(file);
        //标签
        Tag tag = mp3File.getID3v2TagAsv24();
        if (tag != null) {
            return tag.getFirst(FieldKey.TITLE);
        };
        return null;
    }


}
