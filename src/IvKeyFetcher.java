import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class IvKeyFetcher {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法:");
            System.out.println("  java -jar IvKeyFetcher.jar -ivkey <文件名.dmg> <系统版本或BuildID> <设备型号>");
            System.out.println("  java -jar IvKeyFetcher.jar -e <系统版本号> <设备型号>");
            return;
        }

        // ✅ 模式 1：查询 IVKey
        if (args[0].equalsIgnoreCase("-ivkey")) {
            if (args.length < 4) {
                System.out.println("用法: java -jar IvKeyFetcher.jar -ivkey <文件名.dmg> <系统版本或BuildID> <设备型号>");
                return;
            }

            String fullPath = args[1];
            String dmgFile = fullPath.contains("/") ? fullPath.substring(fullPath.lastIndexOf("/") + 1) : fullPath;
            String versionOrBuild = args[2];
            String deviceModel = args[3];

            HashMap<String, String> buildToVersion = loadVersionMap();

            String systemVersion = buildToVersion.getOrDefault(versionOrBuild, versionOrBuild);

            String githubBaseUrl = "https://raw.githubusercontent.com/PlanePlace/FirmwareIVKey/main/";
            String fileName = dmgFile;
            String ivkeyUrl = githubBaseUrl + deviceModel + "/" + systemVersion + "/" + fileName + ".json";

            try {
                URL url = new URL(ivkeyUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line.trim());
                    reader.close();

                    String ivkey = sb.toString().replace("\"", "").trim();
                    System.out.println(ivkey);
                } else if (conn.getResponseCode() == 404) {
                    System.out.println("❌ 未找到对应的 JSON 文件，请检查路径是否正确。");
                } else {
                    System.out.println("⚠️ 请求失败，HTTP 状态码: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                System.out.println("⚠️ 获取 ivkey 过程中出错: " + e.getMessage());
            }
        }

        // ✅ 模式 2：检测 version_buildid.json 是否包含系统版本
        else if (args[0].equalsIgnoreCase("-e")) {
            if (args.length < 3) {
                System.out.println("用法: java -jar IvKeyFetcher.jar -e <系统版本号> <设备型号>");
                return;
            }

            String versionToCheck = args[1];
            String deviceModel = args[2]; // 暂时未用到，但预留参数位置

            HashMap<String, String> versionMap = loadVersionMap();

            // 判断是否存在该版本号
            boolean exists = versionMap.containsValue(versionToCheck);

            System.out.println(exists ? "true" : "false");
        }

        else {
            System.out.println("未知参数: " + args[0]);
        }
    }

    // ✅ 从 jar 内读取 JSON 映射表
    private static HashMap<String, String> loadVersionMap() {
        HashMap<String, String> map = new HashMap<>();
        try (InputStream in = IvKeyFetcher.class.getResourceAsStream("/version_buildid.json")) {
            if (in == null) {
                System.out.println("⚠️ 未找到内置的 version_buildid.json 文件。");
            } else {
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(new InputStreamReader(in));
                for (Object key : json.keySet()) {
                    map.put((String) key, (String) json.get(key));
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ 加载 version_buildid.json 出错：" + e.getMessage());
        }
        return map;
    }
}