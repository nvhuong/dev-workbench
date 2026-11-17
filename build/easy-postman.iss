; EasyPostman Inno Setup kịch bản
; For detailed documentation, please refer to:https://jrsoftware.org/ishelp/

; ==================== Các biến được truyền vào từ bên ngoài ====================
; Các biến này có thể được truyền vào thông qua dòng lệnh: .iscc /DMyAppVersion=4.0.9 easy-postman.iss
; LƯU Ý: Đường dẫn có liên quan đến  ISS vị trí tập lệnh (build thư mục), vì vậy cần thiết ..\ Trở lại cấp độ trước đó
#ifndef MyAppVersion
  #define MyAppVersion "4.0.9"
#endif
#ifndef MyAppSourceDir
  #define MyAppSourceDir "..\target\EasyPostman"
#endif
#ifndef MyOutputDir
  #define MyOutputDir "..\dist"
#endif
#ifndef MyArch
  #define MyArch "x64"
#endif

; ==================== Áp dụng định nghĩa hằng ====================
#define MyAppName "EasyPostman"
#define MyAppPublisher "Laker"
#define MyAppURL "https://github.com/lakernote/easy-postman"
#define MyAppSupportURL "https://github.com/lakernote/easy-postman/issues"
#define MyAppUpdatesURL "https://github.com/lakernote/easy-postman/releases"
#define MyAppExeName "EasyPostman.exe"

[Setup]
; AppId là mã định danh duy nhất của ứng dụng (GUID），Đừng sửa đổi nó! Được sử dụng để hỗ trợ cài đặt nâng cấp
; Lưu ý:AppId Sử dụng trực tiếp GUID（không có dấu ngoặc nhọn).Inno Setup sẽ được thêm tự động _is1 Suffix tạo khóa đăng ký
; Đường dẫn đăng ký cuối cùng: HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\8B9C5D6E-7F8A-9B0C-1D2E-3F4A5B6C7D8E_is1
AppId=8B9C5D6E-7F8A-9B0C-1D2E-3F4A5B6C7D8E
; Thông tin ứng dụng cơ bản
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppSupportURL}
AppUpdatesURL={#MyAppUpdatesURL}
; Đường dẫn cài đặt mặc định: .C:\Program Files\EasyPostman
DefaultDirName={autopf}\{#MyAppName}
; Biểu tượng được hiển thị bởi trình gỡ cài đặt
UninstallDisplayIcon={app}\{#MyAppExeName}
; Các kiến trúc được hỗ trợ:x64 và ARM64（Windows 11 on ARM）
ArchitecturesAllowed=x64compatible arm64
ArchitecturesInstallIn64BitMode=x64compatible arm64
; Không được hiển thị"Chọn thư mục menu Bắt đầu"trang
DisableProgramGroupPage=yes
; Cấu hình đầu ra
OutputDir={#MyOutputDir}
OutputBaseFilename={#MyAppName}-{#MyAppVersion}-windows-{#MyArch}
; Cấu hình nén: Sử dụng mức nén cao nhất
Compression=lzma2/max
SolidCompression=yes
; Kiểu giao diện trình hướng dẫn cài đặt:modern（căn hộ hiện đại)
WizardStyle=modern
; Biểu tượng của trình cài đặt (liên quan đến  ISS đường dẫn đến tập lệnh)
SetupIconFile=..\assets\win\EasyPostman.ico

[Languages]
; Ngôn ngữ hướng dẫn cài đặt
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
; Tác vụ tùy chọn: Tạo lối tắt trên màn hình (được chọn theo mặc định)
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
; Sao chép jpackage Tạo tất cả các tập tin vào thư mục cài đặt
Source: "{#MyAppSourceDir}\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#MyAppSourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; Tạo lối tắt
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[InstallDelete]
; Dọn dẹp các phiên bản cũ của tệp trong quá trình cài đặt nâng cấp (để tránh dư thừa tệp và xung đột phiên bản)
Type: files; Name: "{app}\app\*.jar"
Type: filesandordirs; Name: "{app}\runtime\*"
Type: filesandordirs; Name: "{app}\plugins\*"

[Run]
; Tùy chọn khởi động sau khi cài đặt hoàn tất
; Cài đặt tương tác: Nhắc người dùng có nên khởi chạy ứng dụng hay không
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent; Check: ShouldPromptStart
; Cài đặt im lặng: nếu được chỉ định /AUTOSTART các thông số, nền sẽ tự động bắt đầu (để tự động cập nhật cảnh)
Filename: "{app}\{#MyAppExeName}"; Flags: nowait runhidden; Check: ShouldAutoStart

[Code]
{ Kiểm tra xem các tham số dòng lệnh có tồn tại không }
function CmdLineParamExists(const Value: string): Boolean;
var
  I: Integer;
begin
  Result := False;
  for I := 1 to ParamCount do
    if CompareText(ParamStr(I), Value) = 0 then
    begin
      Result := True;
      Exit;
    end;
end;

{ Xác định xem ứng dụng có nên được khởi chạy tự động hay không }
{ Tình huống sử dụng:EasyPostman-Setup.exe /VERYSILENT /AUTOSTART }
function ShouldAutoStart: Boolean;
begin
  Result := WizardSilent and CmdLineParamExists('/AUTOSTART');
end;

{ Xác định xem người dùng có được nhắc khởi chạy ứng dụng hay không (khi cài đặt tương tác) }
function ShouldPromptStart: Boolean;
begin
  Result := not WizardSilent;
end;
