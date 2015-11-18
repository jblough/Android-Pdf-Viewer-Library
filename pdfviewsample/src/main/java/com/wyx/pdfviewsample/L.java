package com.wyx.pdfviewsample;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Log record tool
 */
@SuppressWarnings({ "unused", "ResultOfMethodCallIgnored" }) public class L {

  private static final int LOG_CAT_MAX_LENGTH = 3900;

  private static final String TAG_LINE_BREAK = "****";
  private static final String EMPTY_LOG = "---";

  private static final String ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
  private static final String FILE_NAME = "logger.log";
  private static final int WRITE_TO_SD_PRIORITY_LEVEL = Log.DEBUG;

  private static String logFile = ROOT + "/" + FILE_NAME;
  private static boolean write2SdCard = false;
  private static int write2SdPriorityLevel = WRITE_TO_SD_PRIORITY_LEVEL;

  private static boolean debug = true;

  public static void setDebug(boolean debug) {
    L.debug = debug;
  }

  public static void setWrite2SdCard(boolean sdCard) {
    write2SdCard = sdCard;
  }

  public static void setWriteToSdPriorityLevel(int level) {
    write2SdPriorityLevel = level;
  }

  public static void exception(Throwable e) {
    if (debug && e != null) {
      e.printStackTrace();
    }
  }

  public static void exception(Throwable e, String s) {
    if (debug && e != null) {
      e.printStackTrace();
      e(TAG_LINE_BREAK, s);
    }
  }

  public static void w(Object object, Object msg) {
    if (debug) {
      print(Log.WARN, object, msg);
    }
  }

  public static void w(Object msg) {
    if (debug) {
      print(Log.WARN, TAG_LINE_BREAK, msg);
    }
  }

  public static void v(Object object, Object msg) {
    if (debug) {
      print(Log.VERBOSE, object, msg);
    }
  }

  public static void v(Object msg) {
    if (debug) {
      print(Log.VERBOSE, TAG_LINE_BREAK, msg);
    }
  }

  public static void d(Object object, Object msg) {
    if (debug) {
      print(Log.DEBUG, object, msg);
    }
  }

  public static void d(Object msg) {
    if (debug) {
      print(Log.DEBUG, TAG_LINE_BREAK, msg);
    }
  }

  public static void i(Object object, Object msg) {
    if (debug) {
      print(Log.INFO, object, msg);
    }
  }

  public static void i(Object msg) {
    if (debug) {
      print(Log.INFO, TAG_LINE_BREAK, msg);
    }
  }

  public static void e(Object object, Object msg) {
    if (debug) {
      print(Log.ERROR, object, msg);
    }
  }

  public static void e(Object msg) {
    if (debug) {
      print(Log.ERROR, TAG_LINE_BREAK, msg);
    }
  }

  private static void print(int priority, Object tag, Object msg) {
    String s = toString(msg);
    printToLogCat(priority, tag, s);
    if (write2SdCard) {
      writeLog(priority, tag, s);
    }
  }

  private static void printToLogCat(int priority, Object tag, String s) {
    if (s.length() > LOG_CAT_MAX_LENGTH) {
      println(priority, tag, "log length - " + String.valueOf(s.length()));
      int chunkCount = s.length() / LOG_CAT_MAX_LENGTH;     // integer division
      for (int i = 0; i <= chunkCount; i++) {
        int max = LOG_CAT_MAX_LENGTH * (i + 1);
        if (max >= s.length()) {
          println(priority, "chunk " + i + " of " + chunkCount, s.substring(LOG_CAT_MAX_LENGTH * i, s.length()));
        } else {
          println(priority, "chunk " + i + " of " + chunkCount, s.substring(LOG_CAT_MAX_LENGTH * i, max));
        }
      }
    } else {
      println(priority, tag, s);
    }
  }

  public static void resetLogFile() {
    File file = new File(logFile);
    file.delete();
    try {
      file.createNewFile();
    } catch (IOException e) {
      exception(e);
    }
  }

  private static void writeLog(int priority, Object tag, String s) {
    if (TextUtils.isEmpty(s)) {
      return;
    }

    if (priority < write2SdPriorityLevel) {
      return;
    }

    try {
      File file = new File(logFile);
      if (!file.exists()) {
        file.createNewFile();
      }
      FileWriter writer = new FileWriter(file, true);
      writer.flush();
      writer.close();
    } catch (IOException e) {
      exception(e);
    }
  }

  private static void println(int priority, Object tag, String s) {
    Log.println(priority, getTagName(tag), s);
  }

  private static String getTagName(Object tag) {
    if (tag instanceof String) {
      return (String) tag;
    }

    if (tag instanceof Class<?>) {
      return ((Class<?>) tag).getSimpleName();
    } else {
      return getTagName(tag.getClass());
    }
  }

  private static String toString(Object msg) {
    if (msg == null) {
      return EMPTY_LOG;
    }
    String s = msg.toString();
    if (s.isEmpty()) {
      return EMPTY_LOG;
    } else {
      return s;
    }
  }

  public static void printTouchEvent(MotionEvent ev) {
    L.e("touch event", actionToString(ev.getAction()));
    final int pointerCount = ev.getPointerCount();
    for (int i = 0; i < pointerCount; i++) {
      L.d("point",
          "id[" + i + "]=" + ev.getPointerId(i) + ", x[" + i + "]=" + ev.getX(i) + ", y[" + i + "]=" + ev.getY(i));
    }
    //        L.d("pointer count", pointerCount);
  }

  public static String actionToString(int action) {
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        return "ACTION_DOWN";
      case MotionEvent.ACTION_UP:
        return "ACTION_UP";
      case MotionEvent.ACTION_CANCEL:
        return "ACTION_CANCEL";
      case MotionEvent.ACTION_OUTSIDE:
        return "ACTION_OUTSIDE";
      case MotionEvent.ACTION_MOVE:
        return "ACTION_MOVE";
      case MotionEvent.ACTION_HOVER_MOVE:
        return "ACTION_HOVER_MOVE";
      case MotionEvent.ACTION_SCROLL:
        return "ACTION_SCROLL";
      case MotionEvent.ACTION_HOVER_ENTER:
        return "ACTION_HOVER_ENTER";
      case MotionEvent.ACTION_HOVER_EXIT:
        return "ACTION_HOVER_EXIT";
    }
    int index = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_POINTER_DOWN:
        return "ACTION_POINTER_DOWN(" + index + ")";
      case MotionEvent.ACTION_POINTER_UP:
        return "ACTION_POINTER_UP(" + index + ")";
      default:
        return Integer.toString(action);
    }
  }
}
