alter table spotify_account
    alter column app_user_id type uuid
    using nullif(app_user_id, '')::uuid;

create unique index uq_spotify_account_app_user_id
    on spotify_account (app_user_id);
