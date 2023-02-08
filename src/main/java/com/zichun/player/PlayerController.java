package com.zichun.player;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;


import com.leewyatt.rxcontrols.controls.RXLrcView;
import com.leewyatt.rxcontrols.pojo.LrcDoc;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import com.leewyatt.rxcontrols.controls.RXMediaProgressBar;
import javafx.util.Duration;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;


public class PlayerController {

    /******成员变量******/
    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button musicAddBtn;

    @FXML
    private ImageView musicImageView;

    @FXML
    private Slider musicVolumeSilider;

    @FXML
    private RXLrcView musicLrcView;

    @FXML
    private RXMediaProgressBar musicProgressBar;

    @FXML
    private Label musicTimeLabel;

    @FXML
    private StackPane nextMusicBtn;

    @FXML
    private ToggleButton playMusicBtn;

    @FXML
    private StackPane preMusicBtn;

    @FXML
    private ToggleButton randomMusicBtn;

    @FXML
    private ToggleButton singleMusicBtn;

    @FXML
    private BorderPane playerPane;//主界面borderPane

    @FXML
    private AnchorPane topPane;


    @FXML
    private AnchorPane leftPane;//实现歌曲的拖拽添加

    @FXML
    private Text musicSongNameView;


    @FXML
    private ListView<File> musicListView;//为List数组设置file类型

    private MediaPlayer player;//将MediaPLayer定义为全局变量，不会被回收

    //播放模式的全局变量
    private int singleMusicMod;

    private int randomMusicMod;


    //用于初始化任何控件
    @FXML
    void initialize() {
        initMusicPlayer();
        initMusicListView();
    }


    //初始化音乐播放器
    private  void initMusicPlayer() {
        //对listview的选择属性添加改变事件监听
        musicListView.getSelectionModel().selectedItemProperty().addListener((ob,oldMusic,newMusic)->{
            if(musicListView.getItems().size()==0){
                player.stop();
                player.dispose();
                player=null;
                musicLrcView.setLrcDoc(null);
                musicImageView.setImage(null);
                musicTimeLabel.setText("00:00/00:00");
                musicSongNameView.setText(null);
            }
            if(newMusic!=null) {
                //在新建前一定要判断并disposse，不然多个Mediaplayer对象会造成多个音乐同时播放
                if (player != null) {
                    player.stop();
                    player.dispose();
                    player = null;
                    musicLrcView.setLrcDoc(null);
                    musicImageView.setImage(null);
                    musicTimeLabel.setText(null);
                    musicSongNameView.setText(null);
                }
                //监听到新选择的元素部位不为空，则新建MediaPlayer
                //toURI返回的是绝对路径，因为MediaPlayer是为网络播放器设计的
                player = new MediaPlayer(new Media(MusicData.getMusicPathURI(newMusic)));
                playMusicBtn.setSelected(false);//默认正在播放

                //歌词显示
                lrcViewSet(newMusic);

                //歌曲图片显示
                musicImageViewSet(newMusic);

                //面板有两个滑动控件，为了和播放的音乐进行互动，需要设置监听器
                //Slider改变时音量也该同时变化,添加一个监听器
                volumeSliderSet();
                audioProgressSet();
                //音乐时间
                audioTimeLabelSet();
                //当播放结束根据播放模式做出音乐选择
                playerModeSet();

                player.play();
            }
        });
    }


    ////////对listView中的元素进行修改///////
    //调用了setCellFactory方法来重新定义列表单元的实现类
    private  void initMusicListView() {
        //setCellFactory解决单元格展示效果的问题，UI控件的自定义
        //https://blog.csdn.net/moakun/article/details/83051790

        musicListView.setCellFactory(new Callback<ListView<File>, ListCell<File>>() {
            //我们通过继承ListCell，重载UpdateItem方法，来自己定义每一个Cell的内容。
            @Override
            public ListCell<File> call(ListView<File> fileListView) {
                //初始化一个新的ListCell元素
                return new ListCell<>(){
                    //自定义UpdateItem单元格中的内容
                    @Override
                    protected void updateItem(File file, boolean empty) {
                        super.updateItem(file, empty);
                        //重载updateItem方法
                        if(file!=null){
                            cellCustom(file);
                        }
                        else{
                            setGraphic(null);
                        }
                    }

                    private void cellCustom(File file) {
                        //将cell文字更新为只有歌曲名字,删除最后四位为.xxx格式的文字
                        BorderPane borderPane = new BorderPane();
                        //建立一个BorderPane界面，在其中放入控件
                        Label dispMusicSong = new Label();
                        //设置为歌曲名字
                        dispMusicSong.setText(MusicData.getMusicSrcName(file));
                        Button delMusicBtn = new Button();
                        //delMusicBtn.setText("删除");
                        delMusicBtn.getStyleClass().add("music-del-btn");
                        delMusicBtn.setGraphic(new Region());//就是fxml中子项放一个Region
                        delMusicBtn.setOnAction(actionEvent-> {
                            //删除选中的元素,getItem()返回的是这一个cell元素
                            musicListView.getItems().remove(getItem());
                        });
                        borderPane.setCenter(dispMusicSong);
                        borderPane.setRight(delMusicBtn);
                        //delMusicBtn.;
                        //将cell以borderPane显示出来
                        setGraphic(borderPane);
                    }
                };
            }
        });

    }



    //播放器的一些控件设置



    //设置音量条的变化，随之改变播放器的音量
    private void volumeSliderSet() {
        //player的音量与slider的valueProperty
        player.setVolume(musicVolumeSilider.valueProperty().doubleValue()/100);

        musicVolumeSilider.valueProperty().addListener((ob1,oldVol,newVol)->{
            if(player!=null){
                player.setVolume(newVol.doubleValue()/100);
            }

        });
    }

    //播放器进度条控制功能
    private void audioProgressSet() {
        //每一次将进度条的总时长和这首歌的总时长绑定
        if(player!=null){
            musicProgressBar.durationProperty().bind(player.getMedia().durationProperty());
            //点击或者拖动进度条时会同时改变音乐时间
            //seek将播放器搜索到新的播放时间
            musicProgressBar.setOnMouseClicked(event -> {
                //只有player不为空，操作才有效
                if(player!=null){
                    player.seek(musicProgressBar.getCurrentTime());
                }
            });
            musicProgressBar.setOnMouseDragged(event -> { //必须要设置拖动事件，不然会出现进度条反复跳动bug
                if(player!=null){
                    player.seek(musicProgressBar.getCurrentTime());
                }
            });
            //设置监听器，将这首歌播放的位置获取并设置到进度条上
            //不能对绑定的数据进行修改，下方方法是错误的
            //musicProgressBar.currentTimeProperty().bind(player.currentTimeProperty());
            player.currentTimeProperty().addListener((ob2,oldTime,newTime)-> {
                musicProgressBar.setCurrentTime(newTime);
            });
        }
    }

    //播放模式的选择
    private void playerModeSet() {
        player.setOnEndOfMedia(()->{
            if(singleMusicMod ==1){
                player.seek(player.getStartTime());
            }
            else{
                //根据播放模式决定播放顺序
                if(randomMusicMod==1){
                    int musicListSize = musicListView.getItems().size();
                    Random r = new Random();
                    int musicRandomIndex = r.nextInt(musicListSize-1);
                    //System.out.println("musicListSize:"+musicListSize);
                    //System.out.println("随机音乐下标"+musicRandomIndex);
                    musicListView.getSelectionModel().select(musicRandomIndex);
                }
                else{
                    playNextMusic();
                }
            }
        });
    }


    //播放器音乐时间功能
    private void audioTimeLabelSet() {
        //因为输出的时间为毫秒，所以需要sdf来进行格式化输出
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        player.currentTimeProperty().addListener((ob,oldTime,newTime)->{
            //System.out.println(player.getStopTime());
            double nowMusicTime = newTime.toMillis();
            //System.out.println(nowMusicTime);
            if(player!=null){
                //当player不为空时我才获取时间，防止报错
                String nowMusicCurrentTimeS = sdf.format(nowMusicTime);//当前时间
                String nowMusicDurationdTimeS = sdf.format(player.getStopTime().toMillis());//获取歌曲总时间
                //System.out.println(nowMusicCurrentTime+nowMusicDurationdTime);
                musicTimeLabel.setText(nowMusicCurrentTimeS+"/"+nowMusicDurationdTimeS);//每当时间改变，就为TimeLabel打上新的音乐时间
            }

        });
    }



    //歌词显示功能


    private void lrcViewSet(File newMusic) {
        File lrcFile = new File(MusicData.getMusicLrcPath(newMusic));//打开歌词文件
        if(lrcFile.exists()){
            byte[] bytes;//获取文件路径字节
            try {
                bytes = Files.readAllBytes(lrcFile.toPath());
                musicLrcView.setLrcDoc(LrcDoc.parseLrcDoc(new String(bytes, EncodingDetect.detect(bytes))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //将歌词显示进度与player的进度绑定
            musicLrcView.currentTimeProperty().bind(player.currentTimeProperty());
        }
        else{
            //如果没有歌词文件则清空
            musicLrcView.setLrcDoc(null);
        }
    }



    //歌曲图片显示功能
    private void musicImageViewSet(File newMusic) {
        //因为实现的无法读取wav图片，所欲对于后缀为wav的退出功能
        if(newMusic.getName().contains(".mp3")){

            musicImageView.setImage(MusicData.getMusicImage(newMusic));

            try {
                musicSongNameView.setText(MusicData.getMusicArtistInfo(newMusic)+"——"+MusicData.getMusicNameInfo(newMusic));
            } catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException |
                     IOException e) {
                throw new RuntimeException(e);
            }
        }

    }


    //顶部右上角界面的操作按钮响应

    @FXML
    void closeAcction(MouseEvent event) {
        Platform.exit();
    }

    @FXML
    void fullAcction(MouseEvent event) {
        Stage stage  = (Stage)playerPane.getScene().getWindow();//获取当前界面的状态
        stage.setFullScreen(!stage.isFullScreen());//如果不是全屏则设置
    }

    @FXML
    void minimizeAcction(MouseEvent event) {
        Stage stage  = (Stage)playerPane.getScene().getWindow();
        stage.setIconified(true);
    }


    //播放器左侧音乐拖拽添加响应
    public void addMusicAction(){
        leftPane.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                leftPane.setOnDragDropped(new EventHandler<DragEvent>() {
                    @Override
                    public void handle(DragEvent dragEvent) {
                        Dragboard db = dragEvent.getDragboard();
                        if(db.hasFiles()){
                            List<File> dragGet = db.getFiles();
                            System.out.println("拖拽得到"+dragGet);
                        }
                    }
                });
            }
        });
    }





    //播放器底部按钮的事件响应

    @FXML
    void nextMusicAction(MouseEvent event) {
        playNextMusic();
    }
    @FXML
    void playMusicAction(MouseEvent event) {
        playMusic();
    }
    @FXML
    void preMusicAction(MouseEvent event) {
        playPreMusic();
    }

    @FXML
    void randomMusicAction(MouseEvent event) {
        if(randomMusicBtn.isSelected()){
            this.randomMusicMod=0;
        }
        else{
            this.randomMusicMod=1;
        }
        System.out.println("随机播放模式"+randomMusicMod);
    }

    @FXML
    void singleMusicAction(MouseEvent event) {
        if(singleMusicBtn.isSelected()){
            this.singleMusicMod=0;
        }
        else{
            this.singleMusicMod=1;
        }
        System.out.println("单曲循环模式："+singleMusicMod);
    }



    private void playNextMusic() {
        int musicListSize = musicListView.getItems().size();
        //没有上下曲时,位置设为开始
        if(musicListSize<=1){
            player.seek(Duration.millis(0));
        }
        else{
            //得到当前播放的歌曲下标
            int nowPlayingIndex = musicListView.getSelectionModel().getSelectedIndex();
                //如果到了数组的最后一个则返回到第一首音乐
                if(nowPlayingIndex==musicListSize-1){
                    nowPlayingIndex = 0;
                }
                else{
                    nowPlayingIndex++;
                }
            musicListView.getSelectionModel().select(nowPlayingIndex);
        }
    }

    private void playMusic() {
        //音乐不空则
        if(player!=null){
            if(playMusicBtn.isSelected()){
                player.play();
            }
            else{
                player.pause();
            }
        }
    }

    private void playPreMusic() {
        int musicListSize = musicListView.getItems().size();
        if(musicListSize<=1){
            player.seek(player.getStartTime());
        }
        else{
            int nowPlayingIndex = musicListView.getSelectionModel().getSelectedIndex();
                if(nowPlayingIndex==0){
                    nowPlayingIndex = musicListSize-1;
                }
                else{
                    nowPlayingIndex--;
                }
            musicListView.getSelectionModel().select(nowPlayingIndex);
        }
    }


    @FXML
    /*-文件选择器的实现，添加音乐-*/

    void addMusicAction(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();//文件选择器
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("音频文件","*.mp3","*.wav")
        );////添加扩展名过滤器，过滤文件

        fileChooser.setTitle("添加音乐");
        //String filePath = "\\src\\main\\resources\\music";
        fileChooser.setInitialDirectory(
                //user.dir指当前用户工作目录
                new File(System.getProperty("user.dir"))
        );
        //多选的文件选择器，接受返回值
        List<File> fileList=fileChooser.showOpenMultipleDialog(playerPane.getScene().getWindow());//需要判断是否添加了文件
        //指向musicListView中保存的List数组
        List<File> musicList = musicListView.getItems();
        //接受的文件会有为空的情况;
        if(fileList!=null){
            fileList.forEach(file -> {
                if(!musicList.contains(file)){ //如果歌词列表MusicList没有这首，则加入到其中
                    musicList.add(file);
                }
            });
        }
    }

}














    /*
    private void lrcViewSet(File file){
        if(!file.exists()){
            return;
        }

        this.musicLrcView.getChildren().clear();
        this.musicLrcView.setLayoutY(50 * 2 - 10);
        this.lrcList.clear();
        this.currentLrcIndex = 0;


        //查找歌词文件
        File lrcFile = new File(MusicData.musicLrcPath(file));
        System.out.println(lrcFile);

        //歌词下标
        int curLrcIndex = 0;
        int lineCount=0;

        if(lrcFile.exists()){
            System.out.println("歌曲存在"+lrcFile);
            //读取歌词的每一行
            try {
                BufferedReader bufIn = new BufferedReader(new InputStreamReader(new FileInputStream(lrcFile),EncodingDetect.detect(lrcFile)));

                String row = null;
                //用while循环，来获取每一行的歌词String
                while((row = bufIn.readLine())!=null){
                //注意，调用一次readLine就会跳转一行

                    if(!row.contains("[") && !row.contains("]")){
                        continue;
                    }
                    if(row.charAt(1)<'0' ||row.charAt(1)>'9'){
                        continue;
                    }

                    //新建一个label存放每一行的歌词放到vbox里面
                    Label lrcLabel = new Label();
                    lrcLabel.getStyleClass().add("lrc-view");

                    //为每一行label设置歌曲文字
                    lrcLabel.setAlignment(Pos.CENTER);
                    lrcLabel.setText(row.substring(11,row.length()));
                    lrcLabel.setFont(new Font("Consolas",25));

                    this.musicLrcView.getChildren().add(lrcLabel);

                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    */


