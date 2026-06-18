import { Card, CardContent, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { User, Mail, Shield } from 'lucide-react';
import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { authService } from '../services/authService';
import type { ProfileResponse } from '../types/api';

export default function Profile() {
  const user = authService.getStoredUser();
  const [profile, setProfile] = useState<ProfileResponse>({
    fullName: user?.fullName || user?.username || '',
    email: user?.email || '',
    profilePhotoDataUrl: undefined,
  });
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const roleLabel = user?.roles?.[0]?.replaceAll('_', ' ') || 'User';

  useEffect(() => {
    let active = true;
    authService.getProfile()
      .then((data) => {
        if (active) {
          setProfile(data);
        }
      })
      .catch(() => {
        if (active) {
          setError('Unable to load profile.');
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const uploadPhoto = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      setProfile((current) => ({ ...current, profilePhotoDataUrl: String(reader.result) }));
      setMessage('Profile photo ready to save.');
    };
    reader.readAsDataURL(file);
  };

  const saveProfile = async () => {
    setSaving(true);
    setError('');
    setMessage('');
    try {
      setProfile(await authService.updateProfile(profile));
      setMessage('Profile changes saved.');
    } catch {
      setError('Unable to save profile.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">My Profile</h1>
      {message && <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div>}
      {error && <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}
      
      <Card>
        <CardHeader>
          <CardTitle>Personal Information</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center gap-6">
            <div className="w-24 h-24 rounded-full bg-slate-200 flex items-center justify-center border-4 border-white shadow-sm overflow-hidden">
              {profile.profilePhotoDataUrl ? (
                <img src={profile.profilePhotoDataUrl} alt="" className="h-full w-full object-cover" />
              ) : (
                <User className="w-10 h-10 text-slate-400" />
              )}
            </div>
            <div>
              <h2 className="text-xl font-bold text-slate-900">{profile.fullName || user?.username}</h2>
              <p className="text-slate-500">{roleLabel}</p>
              <div className="mt-3 flex gap-2">
                <input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={uploadPhoto} />
                <Button variant="outline" size="sm" onClick={() => fileInputRef.current?.click()}>Upload Photo</Button>
                <Button variant="ghost" size="sm" className="text-red-600" disabled={!profile.profilePhotoDataUrl} onClick={() => setProfile((current) => ({ ...current, profilePhotoDataUrl: undefined }))}>Remove</Button>
              </div>
            </div>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-6 border-t border-slate-100">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Full Name</label>
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input type="text" value={profile.fullName} onChange={(event) => setProfile((current) => ({ ...current, fullName: event.target.value }))} className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 focus:bg-white focus:ring-2 focus:ring-blue-500 outline-none" />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Email Address</label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input type="email" value={profile.email} onChange={(event) => setProfile((current) => ({ ...current, email: event.target.value }))} className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm bg-slate-50 focus:bg-white focus:ring-2 focus:ring-blue-500 outline-none" />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Role</label>
              <div className="relative">
                <Shield className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input type="text" value={roleLabel} disabled className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm bg-slate-100 text-slate-500 cursor-not-allowed" />
              </div>
            </div>
          </div>
          
          <div className="pt-4 flex justify-end">
            <Button variant="primary" disabled={saving} onClick={saveProfile}>Save Changes</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
